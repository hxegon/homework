(ns hxegon.homework.read
  (:require
   [hxegon.homework.person :as p]
   [clojure.pprint :refer [print-table]]
   [clojure.set :refer [rename-keys]])
  (:import
   (java.io BufferedReader FileReader)))

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

(defn read-people-file
  "Takes a delimiter key and a filepath string, reads it and returns a map:
  { :people [Person] :errors [{:file string :line integer :msg string}] }"
  [delim file-name]
  (->> (with-open [rdr (BufferedReader. (FileReader. file-name))]
         (into [{:errors [] :people []}]
               (map (fn [line-info]
                      (let [[line line-number] line-info
                            result (p/line->person delim line)]
                        (if (:error-msg result)
                          {:errors {:file file-name :line line-number :msg (:error-msg result)}}
                          {:people result}))))
               (map vector (line-seq rdr) (map inc (range)))))
       (apply merge-with conj))) ; FIXME: This line is inefficient

(defn merge-read-results
  "Merges results from read-people-file, maps of {:people [] :errors []}"
  [results]
  (apply merge-with concat results))

;; TODO : print errors to STDERR. This breaks the tests in a way that's not straightforward to fix...
;; because we would be binding *out* to a stringio in the test but errors would be printed to *err*
;; might have to refactor
(defn print-people-state
  "Print a state map, prints the contained people as a table and any errors unless :silent is true in :options"
  [{:keys [people errors options] :as _state}]
  (let [silent (:silent options)
        readable-people (->> people
                             (map #(update % :dob p/render-dob))
                             (map rename-person-keys))
        error-msgs (when errors (map render-parse-error errors))]
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
        results (merge-read-results (map #(read-people-file (:delimiter %) (:filepath %)) p-files))
        sorter (sort-key p/people-sorters)]
    (print-people-state (merge state (update results :people sorter)))))
