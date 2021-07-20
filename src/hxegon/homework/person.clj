(ns hxegon.homework.person
  (:require
    [clojure.set :refer [rename-keys]]
    [clojure.string :as string]
    [hxegon.homework.internal :refer [key-of-m? safe-parse]])
  (:import
    (java.io BufferedReader FileReader)))

(defrecord Person [lastname firstname gender fav-color dob])

(def dob-format (java.text.SimpleDateFormat. "M/d/yyyy"))

(defn parse-dob
  "Parse a MM/dd/yyyy formatted date string"
  [dob-string]
  (safe-parse dob-format dob-string))

(defn person
  "Constructor for Person with validation. Returns either Person or map with :errors"
  [args]
  (if (not= 5 (count args))
    {:error-msg (str "Expected 5 arguments but got " (count args) " from " (string/join ", " args))}
    (let [[lastname firstname gender fav-color dob-string] args
          dob (parse-dob dob-string)]
      (if (:error-msg dob)
        dob
        (->Person lastname firstname gender fav-color (:token dob))))))

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

(defn line->person
  [delim line]
  (if (key-of-m? delimiters delim)
    (person (string/split line (delim delimiters)))
    {:error-msg (str "Invalid delimiter value: " delim)}))

(defn read-people-file
  "Takes a filename, reads it and returns a map:
  { :people [Person] :errors [{:file string :line integer :msg string}] }"
  [delim file-name]
  (->> (with-open [rdr (BufferedReader. (FileReader. file-name))]
         (into [{:errors [] :people []}]
               (map (fn [line-info]
                      (let [[line line-number] line-info
                            result (line->person delim line)]
                        (if (:error-msg result)
                          {:errors {:file file-name :line line-number :msg (:error-msg result)}}
                          {:people result}))))
               (map vector (line-seq rdr) (map inc (range)))))
       (apply merge-with conj))) ; FIXME: This line is inefficient

(defn read-people-files
  "Reads a collection of people files and merges the results"
  [delim file-names]
  (->> file-names
       (map #(read-people-file delim %))
       (apply merge-with concat)))

(def people-sorters
  {:lastname
   (fn lastname-sorter [people]
     (reverse (sort-by :lastname people)))
   :birthdate
   (fn birthdate-sorter [people]
     (sort-by :dob people))
   :gender 
   (fn gender-sorter [people]
     (->> people
          (sort-by :lastname)
          (sort-by (comp string/lower-case :gender))))
   :name
   (fn name-sorter [people]
     (sort-by #(->> %
                    ((juxt :firstname :lastname))
                    (string/join " ")
                    string/lower-case)
              people))})

(defn rename-person-keys
  "Updates :people keys to readable strings. Intended for use with print-table."
  [person]
  (let [key-map {:firstname "first name"
                 :lastname "last name"
                 :gender "gender"
                 :fav-color "favorite color"
                 :dob "birthdate"}]
    (rename-keys person key-map)))

; Possible "Renderable" protocol for parse-error and java.util.Date

(defn render-parse-error
  "Returns a readble string version of an error map (keys :file :line :msg)"
  [{:keys [file line msg]}]
  (format "In file %s, line %d: %s" file line msg))

(defn render-dob
  "Renders a person's dob as M/D/YYYY"
  [date]
  (.format dob-format date))

