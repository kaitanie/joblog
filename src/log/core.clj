(ns log.core
  (:use log.sgeimporter)
  (:use log.analyzer)
  (:gen-class))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defn -main [ & args ]
  (if (= (count args) 0)
    (println "Too few args")
    (let [command (first args)
          params (rest args)]
      (cond (= command "convert") (let [source (first params)
                                        target (first (rest params))]
                                    (process-file-2 source target))
            (= command "countjobs") (let [datafile (first params)
                                      totaljobs (process-data-try-catch "/home/mael/src/log/log/korundi.pb" (log.analyzer.TotalJobs.))]
                                      (println (str "Total number of jobs: " totaljobs)))
            true (println (str "Unknown command and args: " args))))))
