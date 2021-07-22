(ns hxegon.homework.cli
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.pprint :refer [print-table]]
    [hxegon.homework.person :as p]))

(defn- filepath-exists? [pathstr]
  (.exists (io/file pathstr)))

(defn- parse-keyword [s]
  (->> s string/lower-case keyword))

(def options
  [["-f" "--file FILE" "Input file, with the order of fields being last name, first name, gender, favorite color, and birthdate"
    :multi true ; Can specify multiple files
    :default []
    :update-fn conj
    :validate [filepath-exists? "Input file doesn't exist"]]

   ["-F" "--filespec FILESPEC" "a colon separated specification of delimiter:filepath. Use this if you need to read multiple files with different delimiters"
    :multi true
    :default []
    :update-fn conj
    :parse-fn #(->> (string/split % #":")
                    (zipmap [:delimiter :filepath])
                    ((fn [m] (update m :delimiter parse-keyword))))
    :validate [#(= 2 (count %)) "FILESPEC value should only have one : character"
               (comp filepath-exists? :filepath) "a FILESPEC filepath didn't exist"
               (comp p/valid-delimiter? :delimiter) "a FILESPEC delimiter value wasn't a valid delimiter"]]

   ;; TODO: make default sort method :none, add :none sorter that just returns coll in same order as input
   ["-s" "--sort METHOD" "Sorting method. Either lastname, birthdate, or gender (defaults to lastname)."
    :default :lastname
    :parse-fn parse-keyword
    :validate [p/valid-sorter? "Sorting option must be either lastname, birthdate or gender"]]

   ["-d" "--delimiter DELIM" "Field delimiter keyword. Options are pipe, comma, or space. Defaults to pipe"
    :default :pipe
    :parse-fn parse-keyword
    :validate [p/valid-delimiter? "Delimiter keyword needs to be one of the words pipe, comma or space"]]

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
        "USAGE: program-name <action> [options]"
        ""
        "Actions:"
        "read  - reads files specified by one or more -f flags and prints the people from those files"
        "        relevant options: -S (silent) -d (delimiter) -s (sort-by) -f (file) -F (<delimiter>:<filepath>)"
        "api   - starts an API server, see the relevent section in the README more info"
        "debug - prints the value of initial state. Can use any other options, but not subcommands"
        ""
        "Options:"
        options-summary
        ""
        "Project Page:"
        "https://github.com/hxegon/homework"]
       (string/join \newline)))

(def action-set
  "set of subcommand keywords"
  #{:read
    :debug
    :api})

;; FIXME: api action accepts any options but it doesn't use any
(defn args->initial-state
  "Takes a coll of cli arguments (as in [& args] in -main), and returns an initial application
  state map.
  Keys:
   :action       - a keyword that's member of hxegon.homework.cli/action-set indicating what action the program should take
   :options      - a map of flags and their values as parsed by clojure.tools.cli/parse-opts
   :summary      - a program summary string
   :exit-message - presence indicates the program should exit, printing this message
   :ok?          - boolean indicating whether exit code should be successful or not"
  [args]
  (let [{:keys [options arguments errors summary] :as state} (parse-opts args options)
        action (or (-> arguments first keyword) :read)
        use-msg (usage summary)]
    (cond
      (:help options)
      {:ok? true :exit-message use-msg}
      errors
      {:ok? false :exit-message (error-message errors)}
      (not= 1 (count arguments))
      {:ok? false :exit-message (str "There should only be one action argument" \newline use-msg)}
      (not (action-set action))
      {:ok? false :exit-message (str "Argument " action " isn't a possible action." \newline use-msg)}
      (and (= action :read) (and (->> options :file empty?) (->> options :filespec empty?)))
      {:ok? false :exit-message "You must specify one or more files using -f or --file when you're use read"}
      :else
      (assoc state :action action))))

;; TODO : print errors to STDERR. This breaks the tests in a way that's not straightforward to fix...
;; because we would be binding *out* to a stringio in the test but errors would be printed to *err*
;; might have to refactor
(defn print-people-state
  "Print a state map, prints the contained people as a table and any errors unless :silent is true in :options"
  [{:keys [people errors options] :as _state}]
  (let [silent (:silent options)
        readable-people (->> people
                             (map #(update % :dob p/render-dob))
                             (map p/rename-person-keys))
        error-msgs (when errors (map p/render-parse-error errors))]
    (print-table readable-people)
    (when (and (not silent) (->> errors empty? not))
      (do (println)                 ; empty line between people table and errors
          (doseq [error-msg error-msgs]
            (println error-msg))))))

(defn main
  [{:keys [options] :as state}]
  (let [{files :file
         sort-key :sort
         delim :delimiter
         fspecs :filespec} options
        p-files (concat fspecs (map #(assoc {} :filepath % :delimiter delim) files))
        results (p/merge-read-results (map #(p/read-people-file (:delimiter %) (:filepath %)) p-files))
        sorter (sort-key p/people-sorters)]
    (print-people-state (merge state (update results :people sorter)))))
