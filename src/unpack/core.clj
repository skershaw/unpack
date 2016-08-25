; skip download step?
; http://stackoverflow.com/questions/32742744/how-to-download-a-file-and-unzip-it-from-memory-in-clojure
(ns unpack.core
  (:gen-class)
  (:require [me.raynes.fs.compression :as fsc]
            [clojure.java.io :as io]
            [clojure.string :as s])
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

; (defn files-set
;   "gets a set of unique base file names"
;   [file-list]
;   (set (map file-name-base file-list)))

(defn get-file-versions
  "returns a lazy seq of files in folder whose names start with input string"
  [file-str folder]
  ; (do (println (str "get-file-versions " file-str " " folder))
  (filter #(s/starts-with? % file-str) (archives-in-dir folder)))

; (defn better-compare
;   "compare str lengths, then values, so that abc-12 > abc-2. returns the greater str"
;   ; consider stripping out "-" if naming system is not consistent
;   [a b]
;   (if (= (count a) (count b) )
;     (if (> (compare a b) 0)
;       a
;       b)
;     (if (> (count a) (count b))
;       a
;       b)))

; (defn get-latest-version
;   "get the latest version of file in folder. Not currently used"
;   [filestr folder]
;   (reduce better-compare (get-file-versions filestr folder)))

(defn glv-from-coll
  [coll from]
  {:pre  [(coll? coll)]
   :post [(string? %)]}
  (first (filter #(.exists (io/file (str from %1))) (reverse coll))))

(defn make-dir
  "make directory if it does not exist yet"
  [dir-name]
  {:pre  [(string? dir-name)]}
   ; :post [(.exists (io/file dir-name))]}
  (#(if-not (and (.exists (io/file %1)) (.isDirectory (io/file %1)))
    (.mkdir (io/file %1))) dir-name))

(defn extract
  "supports .tar, .tar.gz, .gz, .zip; returns target dir, which is used in recursion."
  [archive from to]
  {:pre [(.exists (io/file (str from archive)))]}
  (make-dir to)
  (try
    (cond
      (s/ends-with? archive ".zip")
        (fsc/unzip (str from archive) (str to))
      (s/ends-with? archive ".rar")
        (ExtractArchive/extractArchive (io/file (str from archive)) (io/file (str to)))
      (s/ends-with? archive ".tar")
        (do (fsc/untar (str from archive) (str to))
          (str to (file-name-base archive)))
      (s/ends-with? archive ".tar.gz")
        (do (fsc/gunzip (str from archive) (str to (file-name-base archive) ".tar"))
          (fsc/untar (str to (file-name-base archive) ".tar") (str to))
          (io/delete-file (str to (file-name-base archive) ".tar"))
          (str to (file-name-base archive)))
      (s/ends-with? archive ".gz")
        (do (fsc/gunzip (str from archive) (str to))
          (str to))
      :else (println "nothing to extract"))
    (catch Exception e (str "caught exception when trying to extract archive: " (.getMessage e)))))

; (defn extract-recursive
;   ([archive from to]
;   (extract-recursive (extract archive from to)))
;   ([dir]
;   (if (archives-in-dir dir)
;       (map #(extract-recursive % dir dir) (archives-in-dir dir)))))

(defn extract-files
  "assumes input is a list of maps:
  [{:to 'target-dir/' :from 'greatest-common-source-dir/' :files ['path1/file.ext' 'path2/file2.ext' ...]} {...} {...}]
  where the end-most file is the latest version. Extracts the latest existing version."
  [coll]
  {:pre [(and (coll? coll) (map #(map? %) coll))]}
  (pmap #(extract (glv-from-coll (% :files) (or (% :from) "")) (or (% :from) "") (or (% :to) "")) coll))

(defn extract-folder
  [from to]
  {:pre [(and (string? from) (string? to))]}
  (map #(extract % from to) (archives-in-dir from)))

(defn -main
  "see extract-files"
  ([coll]
   (extract-files coll))
  ([from to]
   (extract-folder from to)))

  ; [& {:keys [from to] :or {from dl-directory to target-directory}}]
  ; ; the keys might have an impact on performance
  ; ; (println (str "   " from "   " to ))

  ; (if (> (count (archives-in-dir from)) 0)
  ;   (->> (files-set (archives-in-dir from))
  ;     (map #(get-latest-version % from))
  ;     ; (map #(extract % from (str to (file-name-base %))))
  ;     (map #(extract % from to ))
  ;     (map #(-main :from (str % "/") :to (str % "/")))
  ;     (doall ))))