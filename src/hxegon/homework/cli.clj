(ns hxegon.homework.cli
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.pprint :refer [print-table]]
    [hxegon.homework.internal :refer [key-of-m?]]
    [hxegon.homework.person :as p]))

(def options
  [["-f" "--file FILE" "Input file"
    :multi true ; Can specify multiple files
    :default []
    :update-fn conj ; join values with default into list of files
    :validate [#(.exists (io/file %)) "Input file doesn't exist"]]

   ["-s" "--sort METHOD" "Sorting method. Either lastname, birthdate, or gender (defaults to lastname)."
    :default :lastname
    :parse-fn #(->> % string/lower-case keyword)
    :validate [#(key-of-m? p/people-sorters %) ; input to validate fn is output of parse-fn (a lower case keyword)
               "Sorting option must be either lastname, birthdate or gender"]]

   ["-d" "--delimiter DELIM" "Field delimiter keyword. Options are 'pipe': ' | ', comma: ', ', or space: ' '. Defaults to pipe"
    ; TODO: Change from inputing pattern to inputing symbol to lookup as pattern
    :default (:pipe p/delimiters)
    :parse-fn #(->> % string/lower-case keyword)
    :validate [#(key-of-m? p/delimiters %) "Delimiter keyword needs to be one of the words pipe, comma or space"]]

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

(defn args->initial-state
  "Takes an option map as returned by parse-opts, validates it and returns
  a map indicating the action the program should take. Includes an optional
  status key, :ok?, and an :exit-message key if the program should exit."
  [args]
  (let [{:keys [options _arguments errors summary] :as state} (parse-opts args options)]
    (cond
      (:help options)
      {:ok? true :exit-message (usage summary)}
      errors
      {:ok? false :exit-message (error-message errors)}
      (->> options :file empty?)
      {:ok? false :exit-message "You must specify one or more files using -f or --file"}
      :else state)))

(defn print-people-state
  "Print people as a table, with more readable field names and errors (-> state :options :silent)
  takes keys :people [Person], :errors [{ :file :line :msg }], and :options { :silent bool }"
  [{:keys [people errors options] :as _state}]
  (let [silent (:silent options)
        readable-people (->> people
                             (map #(update % :dob p/render-dob))
                             (map p/rename-person-keys))
        error-msgs (when errors (map p/render-parse-error errors))]
    (print-table readable-people)
    (when (and (not silent) (->> errors empty? not))
      (do (println) ; empty line between people table and errors
          (doseq [error-msg error-msgs]
            (println error-msg))))))

(defn main
  [{:keys [options] :as state}]
  (let [{files :file
         sort-key :sort
         delim :delimiter} options
        result (p/read-people-files delim files)
        sorter (sort-key p/people-sorters)]
    (print-people-state (merge (update result :people sorter) state))))
