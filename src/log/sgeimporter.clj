(ns log.sgeimporter
  (:use protobuf.core
        log.common))

(defn is-whitespace? [c]
  (or (= c " ")
      (= c \space)))

(defn parse-sge-time
  "Turn time \"hh:mm:ss\" into {:hour hh :minute mm :second ss}."
  [s]
  (let [[hh mm ss] (map #(Integer/parseInt %) ;;
                        (filter #(> (count %) 0) (into [] (.split s ":"))))]
    {:hour hh :minute mm :second ss}))

(defn month->number
  "Turn month name into month number: e.g. \"Jan\" -> 1 etc."
  [s]
  (cond (= s "Jan") 1
        (= s "Feb") 2
        (= s "Mar") 3
        (= s "Apr") 4
        (= s "May") 5
        (= s "Jun") 6
        (= s "Jul") 7
        (= s "Aug") 8
        (= s "Sep") 9
        (= s "Oct") 10
        (= s "Nov") 11
        (= s "Dec") 12))

(defn parse-sge-date
  "SGE Prints date in the following format:
   \"Sun Jan  1 00:05:00 2012\"
   This must be parsed to a hash map:
   {:year 2012
    :month 1
    :day 1
    :dayname \"Sun\"
    :hour 00
    :minute 05
    :second 00}
  "
  [s]
  (let [[dayname month-name day time year] (filter #(> (count %) 0) (.split s " "))
        m (month->number month-name)
        d {:year (Integer/parseInt year)
           :dayname dayname
           :month (month->number month-name)
           :day (Integer/parseInt day)}
        t (parse-sge-time time)]
    (conj d t)))

(defn log-value-type-mapper
  "Return type conversion function than converts the field to the
   proper datatype."
  [kw]
  (let [ints      #{:jobnumber :priority :slots
                    :exit_status
                    :ru_maxrss :ru_ixrss :ru_ismrss
                    :ru_isrss :ru_idrss :ru_minflt :ru_majflt :ru_nswap
                    :ru_inblock :ru_outblock :ru_msgsnd :ru_msgrcv :ru_oublock
                    :ru_nsignals :ru_nvcsw :ru_nivcsw}
        floats    #{:cpu :mem :io :iow :ru_utime :ru_stime :ru_wallclock}
        dates     #{:end_time :qsub_time :start_time}
        mem-unit  #{:maxvmem}]
    (cond (not (nil? (ints     kw))) (fn [x] (Integer/parseInt x))
          (not (nil? (floats   kw))) (fn [x] (Float/parseFloat x))
          (not (nil? (dates    kw))) (fn [x] (protobuf DateEntry (parse-sge-date   x)))
          (not (nil? (mem-unit kw))) (fn [x] (Float/parseFloat (->> x
                                                                    reverse
                                                                    rest
                                                                    reverse
                                                                    (apply str))))
          true                       identity))) ;; Fallthrough case: just string

(defn parse-sge-attr-line
  "SGE qacct -j output contains lines like:
   qname      mgrid

   We have to split this string by using space as delimiter (and
   filter out zero length elements). We also use log-value-type-mapper
   function to generate a converter function that changes the value
   string to appropriate type (e.g. int, float or date). We finally
   end up with key-value hash maps like this:
   {:qname \"mgrid\"}
   "
  [s]
  (let [[k rem] [(apply str (take-while #(not (is-whitespace? %)) s)) ;; The first non-whitespace part of the string is the key
                            (drop-while #(not (is-whitespace? %)) s)]
        tmp-v   (apply str (drop-while is-whitespace? rem))           ;; ... and the rest is treated as the value.
        v       (->> (reverse tmp-v)                                  ;; Drop trailing whitespace
                     (drop-while is-whitespace?)
                      reverse
                     (apply str))
        kw      (keyword k)
        type-converter-fn (log-value-type-mapper kw)]
    {kw (type-converter-fn v)}))

(defn parse-sge-attr-block
  "Convert one SGE qacct report block to a hash map by first parsing
   individual lines and then combining the key-value pairs to a single
   record."
  [lines]
  (let [kvs (map parse-sge-attr-line lines)]
    (apply merge kvs))) ;; Merge all key-value pairs into a hash map that represents one log entry.

(defn split-text-block-at-line-containing
  "Split a text to blocks by using some lines as separators (and
  filtering them out). This version uses loop-recur (and is slightly
  imperative looking)."
  [separator txt]
  (loop [acc []
         dat txt]
    (let [l (first dat)]
      (cond (.contains l separator) [acc (rest dat)]
            true                    (let [new-acc (conj acc l)]
                                      (recur new-acc (rest dat)))))))

(defn split-text-block-at-line-containing-lazy
  "Split a text to blocks by using some lines as separators (and
  filtering them out). This version uses lazy seqs instead of
  loop-recur."
  [separator txt]
  (let [x  (take-while #(not (.contains % separator)) txt)
        xs (rest (drop-while #(not (.contains % separator)) txt))] ;; Drop also the separator
    [x xs]))

(defn serialize-jobentry [data-map]
  (protobuf LogEntry data-map))

(defn process-file-2 [filename output-file]
  (let [rdr (java.io.BufferedReader. (java.io.FileReader. filename))
;;        wrr (java.io.BufferedWriter. (java.util.zip.GZIPOutputStream. (java.io.ByteArrayOutputStream. (java.io.FileWriter. "test.pb"))))
        wr  (-> (java.io.FileOutputStream. output-file)
;;                                           java.util.zip.GZIPOutputStream.
                                           java.io.BufferedOutputStream.
                                           java.io.DataOutputStream.)
        wrr wr
;;        wrr                                (com.google.protobuf.CodedOutputStream/newInstance wr)
;;                                           java.io.OutputStreamWriter.
;;                                           java.io.BufferedWriter.)
        lines (line-seq rdr)
        data-lines (take-while #(not (.contains % "Total System Usage")) (rest lines))]
    (loop [dat data-lines
           n   0]
      (let [[x xs] (split-text-block-at-line-containing-lazy "==========" dat)]
;;      (let [[x xs] (split-text-block-at-line-containing "==========" dat)] ;; Crashes with NPE... :(
        (if (> (count x) 0)
          (do
            (let [d          (parse-sge-attr-block x)
                  data-bytes (protobuf-dump (serialize-jobentry d))
                  size       (count data-bytes)]
              (if (= (mod n 1000) 0)
                (println (str "Processing entry " n)))
              (do
                (.writeDelimitedTo (serialize-jobentry d) wrr))
;;                (.writeRawLittleEndian64 wrr size)
;;                (.writeRawBytes wrr data-bytes))
;;                (.writeLong wrr size)
;;                (.write wrr data-bytes))
              (recur xs (inc n))))
          (.close wr))))))

(defn test-2 []
  (process-file-2 "/home/mael/src/log/test.log" "test.pb"))

(defn convert-korundi []
  (time
   (process-file-2 "/home/mael/src/log/accounting-korundi.log" "korundi.pb")))

(defn convert-jade []
  (time
   (process-file-2 "/home/mael/src/log/accounting-jade.log" "jade.pb")))

;; (defn process-file!
;;   ""
;;   [filename processing-fn]
;;   (let [rdr (java.io.BufferedReader. (java.io.FileReader. filename))
;;         lines (line-seq rdr)
;;         data-lines (rest lines)] ;; Drop the first line since it contains the separator
;;     (loop [text-remaining data-lines]
;;       (let [first-line (first text-remaining)]
;;         (cond (.contains fist-line "Total System Usage") 

