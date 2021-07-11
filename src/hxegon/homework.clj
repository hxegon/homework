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

; NOTE: In the initial version, the -d option just accepted a string for use
; as the argument to string/split, but " | " produced unexpected behaviour.
; string/split only accepts a regex, and | is a special character.
; Forcing the user to know about Java's regex escape character rules,
; or the differences between re-pattern and clojure's regex literal (#"pattern")
; struck me as poor UX, so here we are looking up a keyword for a known regex.
(def delimiters
  {:pipe #" \| "
   :comma #", "
   :space #" "})

(def cli-options
  [["-f" "--file FILE" "Input file"
    :multi true ; Can specify multiple files
    :default []
    :update-fn conj ; join values with default into list of files
    :validate [#(.exists (io/file %)) "Input file doesn't exist"]]

   ["-s" "--sort METHOD" "Sorting method. Either lastname, birthdate, or gender (defaults to lastname)."
    :default :lastname
    :parse-fn #(->> % string/lower-case keyword)
    :validate [#(#{(keys people-sorters)} %) ; input to validate fn is output of parse-fn (a lower case keyword)
               "Sorting option must be either lastname, birthdate or gender"]]

   ["-d" "--delimiter DELIM" "Field delimiter keyword. Options are 'pipe': ' | ', comma: ', ', or space: ' '. Defaults to pipe"
    ; TODO: Change from inputing pattern to inputing symbol to lookup as pattern
    :default (:pipe delimiters)
    :parse-fn #(->> % string/lower-case keyword)
    :validate [#((into #{} (keys delimiters)) %) "Delimiter keyword needs to be one of the words pipe, comma or space"]]

   ["-h" "--help"]])

; "" strings can handle multi-line literals, but this vector style construction
; makes newline placement and blank lines more explicit IMO
(defn usage [options-summary]
  (->> ["HOMEWORK: Person information utility"
        ""
        "USAGE: program-name [options]"
        "Options:"
        options-summary
        ""
        "Project Page:"
        "https://github.com/hxegon/homework"]
       (string/join \newline)))

(defn error-message [errors]
  (string/join
    \newline
    (cons "Uh oh! Something went wrong:" errors)))

(defn opts->action
 "Takes an option map as returned by parse-opts, validates it and returns
 a map indicating the action the program should take. Includes an optional
 status key, :ok?, and an :exit-message key if the program should exit."
 [{:keys [options _arguments errors summary]}]
 (cond
   (:help options)
   {:ok? true :exit-message (usage summary)}
   errors
   {:ok? false :exit-message (error-message errors)}
   (->> options :file empty?)
   {:ok? false :exit-message "You must specify one or more files using -f or --file"}
   :else
   {:options options}))

(defn -main
  "For now just prints the parsed option map"
  [& args]
  (print
    (parse-opts args cli-options)))
