(ns hxegon.person
  (:require
    [clojure.set :refer [rename-keys]]
    [clojure.string :as string]
    [clojure.pprint :refer [print-table]]
    [hxegon.internal :refer [safe-parse]])
  (:import
    (java.io BufferedReader FileReader)))

(defrecord Person [lastname firstname gender fav-color dob])

(def dob-format (java.text.SimpleDateFormat. "MM/dd/yyyy"))

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

(defn read-people-file
  "Takes a filename, reads it and returns a map:
  { :people [Person] :errors [{:file string :line integer :msg string}] }"
  [delim file-name]
  (->> (with-open [rdr (BufferedReader. (FileReader. file-name))]
         (into [{:errors [] :people []}]
               (map (fn [line-info]
                      (let [[line line-number] line-info
                            result (person (string/split line delim))]
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
          (sort-by (comp string/lower-case :gender))))})

(defn rename-person-keys
  "Updates :people keys to readable strings. Intended for use with print-table."
  [person]
  (let [key-map {:firstname "first name"
                 :lastname "last name"
                 :gender "gender"
                 :fav-color "favorite color"
                 :dob "birthdate"}]
    (rename-keys person key-map)))

(defn render-parse-error
  "Returns a readble string version of an error map (keys :file :line :msg)"
  [{:keys [file line msg]}]
  (format "In file %s, line %d: %s" file line msg))

(defn render-dob
  "Renders a person's dob as M/D/YYYY"
  [date]
  (.format dob-format date))

(defn print-people
  "Print people as a table, with more readable field names.
  takes a map of { :people [Person] :errors [{ :file :line :msg }] (see: read-people-files)
  and an optional :silent argument which defaults to nil"
  [{:keys [people errors]} & {:as options}]
  (let [silent (or (:silent options) false)
        readable-people (->> people
                             (map #(update % :dob render-dob))
                             (map rename-person-keys))
        error-msgs (when errors (map render-parse-error errors))]
    (print-table readable-people)
    (when (and (not silent) (->> errors empty? not))
      (do (println) ; empty line between people table and errors
          (doseq [error-msg error-msgs]
            (println error-msg))))))
