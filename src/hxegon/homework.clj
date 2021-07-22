(ns hxegon.homework
  (:require
    [hxegon.homework.cli :as cli]
    [hxegon.homework.api :as api]
    [hxegon.homework.read :as r]
    [clojure.pprint :refer [pprint]])
  (:gen-class))

(defn exit [ok msg]
  (println msg)
  (System/exit (if ok 0 1)))

(defn -main
  "Parses args into initial state and either exits with a message or runs an
  action, passing the state to the action"
  [& args]
  (let [state (cli/args->initial-state args)
        {:keys [ok? exit-message action]} state]
    (if exit-message
      (exit ok? exit-message)
      (case action
        :read (r/main state)
        :api (api/main state)))))
