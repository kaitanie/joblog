(ns log.sgeimporter-test
  (:use [log.sgeimporter])
  (:use [clojure.test])
  (:use [midje.sweet]))

(def date1 "Sun Jan  1 00:04:25 2012")
(def date2 "Thu Sep 20 15:34:52 2012")

(fact (is-whitespace? " ") => true)
(fact (is-whitespace? \space) => true)
(fact (is-whitespace? "a") => false)

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
