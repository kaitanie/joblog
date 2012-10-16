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

(fact "String \" \" is whitespace" (is-whitespace? " ") => true)
(fact "Char space is whitespace" (is-whitespace? \space) => true)
(fact "String \"a\" is not whitespace" (is-whitespace? "a") => false)
(fact "Char a is not whitespace" (is-whitespace? \a) => false)

(fact (month->number "Jan") => 1)
(fact (month->number "Feb") => 2)
(fact (month->number "Mar") => 3)
(fact (month->number "Apr") => 4)
(fact (month->number "May") => 5)
(fact (month->number "Jun") => 6)
(fact (month->number "Jul") => 7)
(fact (month->number "Aug") => 8)
(fact (month->number "Sep") => 9)
(fact (month->number "Oct") => 10)
(fact (month->number "Nov") => 11)
(fact (month->number "Dec") => 12)

(fact (parse-sge-date date1) => {:second 25, :minute 4, :hour 0, :year 2012, :dayname "Sun", :month 1, :day 1})
(fact (parse-sge-date date2) => {:second 52, :minute 34, :hour 15, :year 2012, :dayname "Thu", :month 9, :day 20})
(fact (parse-sge-attr-line "qname     mgrid") => {:qname "mgrid"})
(fact (parse-sge-attr-line "qsub_time    Sun Jan  1 00:04:25 2012") => {:qsub_time {:dayname "Sun" :year 2012 :month 1 :day 1 :hour 0 :minute 4 :second 25}})

(fact (parse-sge-attr-block (line-seq (java.io.BufferedReader. (java.io.StringReader. sge-entry1)))) => parsed-sge-entry-1)
