(ns hxegon.homework
  (:require
    [hxegon.homework.cli :as cli]
    [hxegon.homework.api :as api]
    [clojure.pprint :refer [pprint]])
  (:gen-class))

(defn exit [ok msg]
  (println msg)
  (System/exit (if ok 0 1)))

(defn -main
  "Parses args and either exits or dispatches to main functions of backends"
  [& args]
  (let [state (cli/args->initial-state args)
        {:keys [ok? exit-message action]} state]
    (if exit-message
      (exit ok? exit-message)
      (case action
        :read
        (cli/main state)
        :api
        (api/main state)
        :debug
        (pprint state)))))
