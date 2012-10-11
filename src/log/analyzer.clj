(ns log.analyzer
  (:use protobuf.core)
  (:use log.common)
  (:use [clj-time.core :as clj-time])
  (:use [clj-histogram core histo1d]))

(defn make-date [d]
  (let [s (:second d)
        m (:minute d)
        h (:hour d)
        day  (:day d)
        month (:month d)
        year (:year d)]
    (clj-time/date-time year month day h m s)))

;;(defn job-length-minutes

(defn make-reader [filename]
  (let [io (->> (java.io.FileInputStream. filename)
                java.io.BufferedInputStream.
                java.io.DataInputStream.)
        coded-stream (com.google.protobuf.CodedInputStream/newInstance io)]
  {:io-stream    io
   :coded-stream coded-stream}))

(defn close-reader [rdr]
  (.close (:io-stream rdr)))

(defn get-next-entry-old [rdr]
  (let [s (:coded-stream rdr)]
    (try
      (let [len (.readRawLittleEndian64 s)]
        (let [obj (.readRawBytes s len)]
          (protobuf-load LogEntry obj)))
      (catch java.io.EOFException e
        {})
      (catch com.google.protobuf.InvalidProtocolBufferException e
        (do
          (println "Invalid protocol buffer. Returning empty map.")
          (println (.toString e))
          {})))))

(defn get-next-entry [rdr]
  (let [s (:io-stream rdr)]
    (try
      (if-let [p (protobuf.core.PersistentProtocolBufferMap/parseDelimitedFrom LogEntry s)]
        p
        {})
      (catch java.io.EOFException e
        {})
      (catch com.google.protobuf.InvalidProtocolBufferException e
        (do
          (println "Invalid protocol buffer. Returning empty map.")
          (println (.toString e))
          {})))))


;;  (try
;;    (let [s (:coded-stream rdr)
;;          len (.readRawLittleEndian32 s)
;;          obj (.readRawBytes s len)]
;;      (do
;;        (println (str "Loading " len " bytes."))
;;      (protobuf-load LogEntry obj)))
;;    (catch java.io.EOFException e
;;        {})))

(defprotocol Analyzer
  (analyzer-init [this])
  (analyzer-process-entry [this old-acc entry])
  (analyzer-finalize [this]))

(defrecord JobsInQueue [queue-name]
  Analyzer
  (analyzer-init [_] 0)
  (analyzer-process-entry [_ old-acc entry]
    (if (= (:qname entry) queue-name)
      (+ old-acc 1)
      old-acc))
  (analyzer-finalize [_] nil))

(defrecord TotalJobs []
  Analyzer
  (analyzer-init [_] 0)
  (analyzer-process-entry [_ old-acc entry]
    (inc old-acc))
  (analyzer-finalize [_] nil))

;;(defrecord Histo1D. [:selector :min-value :max-value :bins]
;;  Analyzer
;;  (analyzer-init [this] (make-linear-binning :min-value :bins :max-value))
;;  (analyzer-process-entry [this old-acc entry] (let [bin-index (

(defn process-data [file entry-analyzer]
  (let [reader (make-reader file)]
    (loop [acc (analyzer-init entry-analyzer)
           entry-number 0]
      (let [entry (get-next-entry reader)]
        (cond (= entry {}) (do
                             (analyzer-finalize entry-analyzer)
                             (close-reader reader)
                             acc)
              true  (let [new-acc (analyzer-process-entry entry-analyzer
                                                          acc
                                                          entry)]
                      (recur new-acc (inc entry-number))))))))

(defn process-data-try-catch [file entry-analyzer]
  (try
    (let [reader (make-reader file)]
      (loop [acc (analyzer-init entry-analyzer)
             entry-number 0]
        (let [entry (get-next-entry reader)]
          (do
            (if (= (mod entry-number 10000) 0)
              (println (str "Processing entry " entry-number)))
          (cond (= entry {}) (do
                               (analyzer-finalize entry-analyzer)
                               (close-reader reader)
                               acc)
                true  (let [new-acc (analyzer-process-entry entry-analyzer
                                                            acc
                                                            entry)]
                        (recur new-acc (inc entry-number))))))))
    (catch Exception e
      (do
        (println e)))))

(defn test-0 []
  (process-data-try-catch "/home/mael/src/log/log/korundi.pb" (TotalJobs.)))

(defn test-1 []
  (process-data-try-catch "/home/mael/src/log/log/korundi.pb" (JobsInQueue. "mgrid")))

;;(defn test-2 []
;;  (let [binning (make-linear-binning 0.0 100 5000.0)]
;;    (process-data-try-catch "/home/mael/src/log/log/korundi.pb" (Histo1D. :selector (fn [entry] (:)))