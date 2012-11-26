(ns log.common
  (:use flatland.protobuf.core)
  (:import Logv1$Joblog)
  (:import Logv1$Date))

(def LogEntry (protodef Logv1$Joblog))
(def DateEntry (protodef Logv1$Date))
