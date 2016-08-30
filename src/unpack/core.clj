(ns unpack.core
  (:gen-class)
  (:require [me.raynes.fs.compression :as fsc]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.core.async :as async])
  (:import [com.github.junrar.testutil ExtractArchive]))

(def dl-directory "zipped-files/")
(def target-directory "zipped-files/extracted-files")

(defn archives-in-dir
  "gets the names of archive files in target dir (returns lazy seq)"
  [dir]
  {:pre [(and (string? dir) (.exists (io/file dir)) (.isDirectory (io/file dir)))]}
  (for [x (seq (.list (io/file dir))) y [".tar" ".gz" ".zip" ".rar"] :when (s/ends-with? x y)] x))

(defn file-name-base
  "strips numbers and file ext from string"
  [file]
  {:pre  [(string? file)]
   :post [(string? %)]}
  (s/replace file #"-?[0-9]*\..*" ""))

(defn get-file-versions
  "returns a lazy seq of files in folder whose names start with input string"
  [folder file-str]
  (filter #(s/starts-with? % file-str) (archives-in-dir folder)))

(defn glv-from-coll
  [coll from]
  ; {:pre  [(coll? coll)]
   ; :post [(string? %)]}
(try (loop [c (reverse coll)]
     (let [f (str from (first c))]
       (if (and (.exists (io/file f)) (not (.isDirectory (io/file f))))
           f
           (do (println "Warning: missing the latest data file:" f)
               (if-not (nil? (next c))
                       (recur (next c))
                       "Warning: no files found.")))))
(catch Exception e "Warning: no files found.")))

(defn make-dir
  "make directory if it does not exist yet"
  [dir]
  {:pre  [(string? dir)]}
   ; :post [(.exists (io/file dir))]}
  (if-not (and (.exists (io/file dir)) (.isDirectory (io/file dir)))
          (.mkdir (io/file dir))))

(defn extract
  "supports .tar, .tar.gz, .gz, .zip; returns target dir, which is used in recursion."
  [archive to del]
  (make-dir to)
  (try
    (cond
      (s/ends-with? archive ".zip")
        (do (fsc/unzip archive to)
            (println "Extracted" archive)
            (if (true? del) (fs/delete archive)))
      ; can't extract rar5. And I cannot suppress the output
      (s/ends-with? archive ".rar")
        (do (ExtractArchive/extractArchive (io/file archive) (io/file (str to)))
            (println "Extracted" archive))
      (s/ends-with? archive ".tar")
        (do (fsc/untar archive (str to))
            (println "Extracted" archive))
      (s/ends-with? archive ".tar.gz")
        (do (fsc/gunzip archive (str (file-name-base archive) ".tar"))
            (fsc/untar (str (file-name-base archive) ".tar") to)
            (io/delete-file (str (file-name-base archive) ".tar"))
            (println "Extracted" archive)
            )
      (s/ends-with? archive ".gz")
        (do (fsc/gunzip archive (str to))
            (println "Extracted" archive))
      :else (println "nothing to extract"))
    (catch Exception e (str "Warning: unable to extract " archive " due to " e)))
  ; (if (true? del) (println (fs/delete archive)) (println "false")))
archive)

(defn dele
  [coll]
   (map #(if (true? (:del %)) (fs/delete (glv-from-coll (:files %) (or (:from %) "")))) coll))

(defn extract-files
  "Assumes input is a coll of maps:
    [{:to 'target-dir/'
      :from 'greatest-common-source-dir/'
      :files ['path1/file.ext' 'path2/file2.ext' ...]
      :del true}
     {...}]
  where the last file is the latest version. Extracts the latest existing version."
  [coll]
  {:pre [(and (coll? coll) (map #(map? %) coll))]}
  (pmap #(extract (glv-from-coll (:files %) (or (:from %) "")) (or (:to %) "") (:del %)) coll))

(defn extract-folder
  [from to starts-with del]
  {:pre [(and (string? from) (string? to))]}
  (map #(extract (str from %) to del) (get-file-versions from (or starts-with ""))))

(defn -main
  ([coll]
   (extract-files coll))
  ([from to & starts-with]
   (extract-folder from to starts-with)))


;; defs for testing

; check each file type
(def a [{:from "zipped-files/" :to "zipped-files/extracted-files/" :files ["testfiles-2.zip"] :del false} {:from "zipped-files/" :to "zipped-files/extracted-files/" :files ["tarta.tar"] :del false} {:from "zipped-files/" :to "zipped-files/extracted-files/" :files ["rar.rar"] :del false} {:from "zipped-files/" :to "zipped-files/extracted-files/" :files ["targz.tar.gz"] :del false}])

; without keys
(def b [{:files ["testfiles-2.zip"]}])
; with keys without values
(def x [{:from "zipped-files/extracted-files/" :to "zipped-files/extracted-files/second-extraction" :files ["testfiles-1.zip"] :del false}])

(def c [{:from "zipped-files/" :to "zipped-files/extracted-files/" :files ["rar.rar" "rar5.rar" "rar6.rar"] :del false}])

(def d [{:from "zipped-files/" :to "zipped-files/extracted-files/" :files ["blablog.zip" "blah.zip" "testfiles-2.zip"] :del false} {:from "zipped-files/extracted-files/" :to "zipped-files/extracted-files/second-extraction" :files ["testfiles-1.zip"] :del false}])

(def e [{:from "zipped-files/" :to "zipped-files/extracted-files/" :files ["rar.rar" "rar5.rar" "rar6.rar"] :del false}])