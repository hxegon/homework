(ns hxegon.homework.cli
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [hxegon.homework.internal :refer [key-of-m?]]
    [hxegon.homework.person :refer [people-sorters delimiters]]))

(def options
  [["-f" "--file FILE" "Input file"
    :multi true ; Can specify multiple files
    :default []
    :update-fn conj ; join values with default into list of files
    :validate [#(.exists (io/file %)) "Input file doesn't exist"]]

   ["-s" "--sort METHOD" "Sorting method. Either lastname, birthdate, or gender (defaults to lastname)."
    :default :lastname
    :parse-fn #(->> % string/lower-case keyword)
    :validate [#(key-of-m? people-sorters %) ; input to validate fn is output of parse-fn (a lower case keyword)
               "Sorting option must be either lastname, birthdate or gender"]]

   ["-d" "--delimiter DELIM" "Field delimiter keyword. Options are 'pipe': ' | ', comma: ', ', or space: ' '. Defaults to pipe"
    ; TODO: Change from inputing pattern to inputing symbol to lookup as pattern
    :default (:pipe delimiters)
    :parse-fn #(->> % string/lower-case keyword)
    :validate [#(key-of-m? delimiters %) "Delimiter keyword needs to be one of the words pipe, comma or space"]]

   ["-S" "--silent" "Suppress any parsing errors and skip the lines that have issues"
    :default false]

   ["-h" "--help"]])

(defn error-message [errors]
  (string/join
    \newline
    (cons "Uh oh! Something went wrong:" errors)))

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
