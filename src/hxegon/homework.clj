(ns hxegon.homework
  (:require [clojure.tools.cli :refer [parse-opts]]
            [hxegon.homework.person :refer [print-people read-people-files people-sorters]]
            [hxegon.homework.cli :as cli])
  (:gen-class))

(defn exit [ok msg]
  (println msg)
  (System/exit (if ok 0 1)))

(defn -main
  "Converts arguments into an 'action' map, and performs actions based on the
  contents."
  [& args]
  (let [action (cli/opts->action (parse-opts args cli/options))
        {:keys [ok? exit-message options]} action
        {files :file
         sort-key :sort
         delim :delimiter} options]
    (if exit-message
      (exit ok? exit-message)
      (let [result (read-people-files delim files)
            sorter (sort-key people-sorters)]
        (print-people (update result :people sorter) :silent (:silent options))))))

