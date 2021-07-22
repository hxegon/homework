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
    {:error-msg (str "Expected 5 values but got " (count args) " from " (string/join ", " args))}
    (let [[lastname firstname gender fav-color dob-string] args
          dob (parse-dob dob-string)]
      (if (:error-msg dob)
        dob
        (->Person lastname firstname gender fav-color (:token dob))))))

(def delimiters
  {:pipe #" \| "
   :comma #", "
   :space #" "})

(defn valid-delimiter? [d]
  (key-of-m? delimiters d))

(defn line->person
  "parse a line into a person. returns either a person or {:error-msg string}"
  [delim line]
  (if (valid-delimiter? delim)
    (person (string/split line (delim delimiters)))
    {:error-msg (str "Invalid delimiter value: " delim)}))

(defn read-people-file
  "Takes a delimiter key and a filepath string, reads it and returns a map:
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
  "Reads a delimiter and a collection of people filepath strings and merges the results"
  [delim file-names]
  (->> file-names
       (map #(read-people-file delim %))
       (apply merge-with concat)))

(def people-sorters
  "map of fns that sort collections of people"
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

(defn valid-sorter? [s]
  (key-of-m? people-sorters s))

(defn rename-person-keys
  "Updates :people keys to readable strings. Intended for use with pprint/print-table."
  [person]
  (let [key-map {:firstname "first name"
                 :lastname "last name"
                 :gender "gender"
                 :fav-color "favorite color"
                 :dob "birthdate"}]
    (rename-keys person key-map)))

(defn render-parse-error
  "Returns a readble string version of an error map ({:file :line :msg})"
  [{:keys [file line msg]}]
  (format "In file %s, line %d: %s" file line msg))

(defn render-dob
  "Renders a date according to dob-format"
  [date]
  (.format dob-format date))

