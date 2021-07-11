(ns hxegon.person
  (:require
    [clojure.string :as string]
    [hxegon.internal :refer [safe-parse]])
  (:import
    (java.io BufferedReader FileReader)))

(defrecord Person [lastname firstname gender fav-color dob])

(defn person
  "Constructor for Person with validation. Returns either Person or map with :errors"
  [args]
  (if (not= 5 (count args))
    {:error-msg (str "Expected 5 arguments but got " (count args))}
    (let [[lastname firstname gender fav-color dob-string] args
          date-format (java.text.SimpleDateFormat. "MM/dd/yyyy")
          dob (safe-parse date-format dob-string)]
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
          (sort-by :gender)))})
