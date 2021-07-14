(ns hxegon.homework
  (:require [clojure.tools.cli :refer [parse-opts]]
            [hxegon.homework.person :refer [print-people read-people-files people-sorters]]
            [hxegon.homework.cli :as cli])
  (:gen-class))

(defn exit [ok msg]
  (println msg)
  (System/exit (if ok 0 1)))

(defn -main
  [& args]
  (let [state (cli/args->initial-state args)
        {:keys [ok? exit-message options]} state
        {files :file
         sort-key :sort
         delim :delimiter} options]
    (if exit-message
      (exit ok? exit-message)
      (let [result (read-people-files delim files)
            sorter (sort-key people-sorters)]
        (print-people (merge (update result :people sorter) state))))))
