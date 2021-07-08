(ns hxegon.homework
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io])
  (:gen-class))

; NOTE: For performance and extensibility reasons this may make more sense as
; a protocol.
(def people-sorters
  {:lastname #()
   :birthdate #()
   :gender #()})

(def cli-options
  [["-f" "--file FILE" "Input file"
    :multi true ; Can specify multiple files
    :default []
    :update-fn conj ; join values with default into list of files
    :validate [#(.exists (io/file %)) "Input file doesn't exist"]]

   ["-s" "--sort=method" "Sorting method. Either lastname, birthdate, or gender (defaults to lastname)."
    :default :lastname
    :parse-fn #(->> % string/lower-case keyword)
    :validate [#(#{(keys people-sorters)} %) ; input to validate fn is output of parse-fn (a lower case keyword)
               "Sorting option must be either lastname, birthdate or gender"]] ; TODO: DRY by generate options from people-sorters
   ["-h" "--help"]])

(defn -main
  "For now just prints the parsed option map"
  [& args]
  (print
    (parse-opts args cli-options)))
