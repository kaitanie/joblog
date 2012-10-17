(ns log.sgeimporter-test
  (:use [log.sgeimporter])
  (:use [clojure.test])
  (:use [midje.sweet]))

(def date1 "Sun Jan  1 00:04:25 2012")
(def date2 "Thu Sep 20 15:34:52 2012")

(def sge-entry1 "qname        mgrid
hostname     compute-0-27.local
group        cmsuser04
owner        cmsuser04
project      NONE
department   cms_users
jobname      https___wms317_
jobnumber    1315567
taskid       undefined
account      sge
priority     10
qsub_time    Tue Oct 16 10:38:58 2012
start_time   Tue Oct 16 11:14:10 2012
end_time     Tue Oct 16 11:25:25 2012
granted_pe   NONE
slots        1
failed       0
exit_status  0
ru_wallclock 675
ru_utime     495
ru_stime     8
ru_maxrss    575780
ru_ixrss     0
ru_ismrss    0
ru_idrss     0
ru_isrss     0
ru_minflt    1056970
ru_majflt    0
ru_nswap     0
ru_inblock   0
ru_oublock   0
ru_msgsnd    0
ru_msgrcv    0
ru_nsignals  0
ru_nvcsw     112339
ru_nivcsw    37414
cpu          503
mem          468.030
io           0.000
iow          0.000
maxvmem      1.475G")

(def parsed-sge-entry-1 {:group "cmsuser04", :ru_msgrcv 0, :account "sge", :ru_nvcsw 112339, :cpu 503.0, :ru_utime 495.0, :iow 0.0, :jobnumber 1315567, :qsub_time {:second 58, :minute 38, :hour 10, :year 2012, :dayname "Tue", :month 10, :day 16}, :ru_ixrss 0, :granted_pe "NONE", :ru_stime 8.0, :ru_msgsnd 0, :ru_inblock 0, :taskid "undefined", :start_time {:second 10, :minute 14, :hour 11, :year 2012, :dayname "Tue", :month 10, :day 16}, :slots 1, :ru_wallclock 675.0, :ru_nivcsw 37414, :ru_idrss 0, :end_time {:second 25, :minute 25, :hour 11, :year 2012, :dayname "Tue", :month 10, :day 16}, :failed "0", :owner "cmsuser04", :hostname "compute-0-27.local", :exit_status 0, :ru_minflt 1056970, :ru_oublock 0, :ru_ismrss 0, :ru_isrss 0, :ru_maxrss 575780, :maxvmem 1.475, :ru_majflt 0, :mem 468.03, :department "cms_users", :ru_nswap 0, :jobname "https___wms317_", :io 0.0, :qname "mgrid", :priority 10, :project "NONE", :ru_nsignals 0})

(def test-parsing-sge-entry-1 (parse-sge-attr-block (line-seq (java.io.BufferedReader. (java.io.StringReader. sge-entry1)))))

(def protobuf-sge-entry-1 (serialize-jobentry test-parsing-sge-entry-1))

(defn is-sge-date? [d]
  (let [day-keys [:day :dayname :month :year :hour :minute :second]]
    (and (=  (count (keys d)) (count day-keys))
         (reduce (fn [a b] (and a b)) (map (fn [x] (not (nil? (d x)))) day-keys)))))

(fact "String \" \" is whitespace" (is-whitespace? " ") => true)
(fact "Char space is whitespace" (is-whitespace? \space) => true)
(fact "String \"a\" is not whitespace" (is-whitespace? "a") => false)
(fact "Char a is not whitespace" (is-whitespace? \a) => false)

(fact (map month->number ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]) => (range 1 13))

(fact (parse-sge-date date1) => {:second 25, :minute 4, :hour 0, :year 2012, :dayname "Sun", :month 1, :day 1})
(fact (parse-sge-date date2) => {:second 52, :minute 34, :hour 15, :year 2012, :dayname "Thu", :month 9, :day 20})
(fact (parse-sge-attr-line "qname     mgrid") => {:qname "mgrid"})
(fact (parse-sge-attr-line "qsub_time    Sun Jan  1 00:04:25 2012") => {:qsub_time {:dayname "Sun" :year 2012 :month 1 :day 1 :hour 0 :minute 4 :second 25}})

(fact test-parsing-sge-entry-1 => parsed-sge-entry-1)
(fact (:group test-parsing-sge-entry-1) => (:group parsed-sge-entry-1))

(fact (:group protobuf-sge-entry-1) => (:group parsed-sge-entry-1))

(fact (count (keys protobuf-sge-entry-1)) => (count (keys parsed-sge-entry-1)))

(defn check-field [description fname]
  (do
    (fact description (not (nil? (fname protobuf-sge-entry-1))) => true)
    ;; Dates are stored in maps that have different types (clojure.lang.PersistentHashMap vs protobuf.core.PersistentProtocolBufferMap)
    (if (not (fname #{:qsub_time :start_time :end_time}))
      (fact "Types match" (type (fname protobuf-sge-entry-1)) => (type (fname parsed-sge-entry-1)))
      (do
        (fact "Date types" (type (fname parsed-sge-entry-1)) => clojure.lang.PersistentHashMap)
        (fact "Date types" (type (fname protobuf-sge-entry-1)) => protobuf.core.PersistentProtocolBufferMap)))
    (fact (str "Checking field " fname) (fname protobuf-sge-entry-1) => (fname parsed-sge-entry-1))))

(check-field ":group types match" :group)
(check-field ":ru_msgrcv types match" :ru_msgrcv)
(check-field "types match" :account)
(check-field "types match" :ru_nvcsw)
(check-field "types match" :cpu)
(check-field "types match" :ru_utime)
(check-field "types match" :iow)
(check-field "types match" :jobnumber)

(fact \""qsub_time is an SGE date" (is-sge-date? (:qsub_time protobuf-sge-entry-1)) => true)
(check-field "types match" :qsub_time)

(check-field "types match" :ru_ixrss)
(check-field "types match" :granted_pe)
(check-field "types match" :ru_stime)
(check-field "types match" :ru_msgsnd)
(check-field "types match" :ru_inblock)
(check-field "types match" :taskid)

(fact "start_time is an SGE date" (is-sge-date? (:start_time protobuf-sge-entry-1)) => true)
(check-field "types match" :start_time)

(check-field "types match" :slots)
(check-field "types match" :ru_wallclock)
(check-field "types match" :ru_nivcsw)
(check-field "types match" :ru_idrss)

(fact "end_time is an SGE date" (is-sge-date? (:end_time protobuf-sge-entry-1)) => true)
(check-field "types match" :end_time)

(check-field "types match" :failed)
(check-field "types match" :owner)
(check-field "types match" :hostname)
(check-field "types match" :exit_status)
(check-field "types match" :ru_minflt)
(check-field "types match" :ru_oublock)
(check-field "types match" :ru_ismrss)
(check-field "types match" :ru_isrss)
(check-field "types match" :ru_maxrss)
(check-field "types match" :maxvmem)
(check-field "types match" :ru_majflt)

(fact "Check :mem field type" (type  (:mem protobuf-sge-entry-1)) => (type  (:mem parsed-sge-entry-1)))
(check-field "types match" :mem)

(check-field "types match" :department)
(check-field "types match" :ru_nswap)
(check-field "types match" :jobname)
(check-field "types match" :io)
(check-field "types match" :qname)
(check-field "types match" :priority)
(check-field "types match" :project)
(check-field "types match" :ru_nsignals)


;;(doseq [k (keys protobuf-sge-entry-1)] (fact "Check field type" (type  (k protobuf-sge-entry-1)) => (type  (k parsed-sge-entry-1))))


;; Check the serialized fields
;;(for [k (keys protobuf-sge-entry-1)] (fact (str "Check the content of field " k) (k protobuf-sge-entry-1) => (k parsed-sge-entry-1)))
