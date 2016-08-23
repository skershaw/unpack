; move .lein to /user
; set up sublime for clojure
; The second answer here should help: http://stackoverflow.com/questions/6259573/how-do-i-access-jar-classes-from-within-my-clojure-program
; https://8thlight.com/blog/colin-jones/2010/11/26/a-leiningen-tutorial.html


; put in error handling (try)
; specify full path of file (take out from), i.e.:
;     (def data-files
;     [[data/something-zipped.zip] [out]
;      [data2/some-file.tar
;       data2/some-newer-version.tar] [out2]])
; option to delete archives
; use pmap to make my code multi-threaded
; take out recursion -- if a zipped file is in an archive, force the user to add two entries into the input instead
; clojure single threaded constraint




; skip download step?
; http://stackoverflow.com/questions/32742744/how-to-download-a-file-and-unzip-it-from-memory-in-clojure
(ns unpack.core
  (:gen-class))

(require '[me.raynes.fs.compression :as fsc] ; https://github.com/Raynes/fs/blob/master/src/me/raynes/fs/compression.clj
         '[clojure.java.io :as io]
         '[junrar :as junrar]
         ; '[java_unrar :as unrar]
         )
; (:import org.clojars.bonega.java-unrar)
; (import com.github.junrar)

(def dl-directory "zipped-files/")
(def target-directory "zipped-files/")



(defn archives-in-dir
    "gets the names of archive files in target dir (returns lazy seq)"
    [dir]
    (for [x (seq (.list (io/file dir))) y [".tar" ".gz" ".zip" ".rar"] :when (clojure.string/ends-with? x y)] x))

(defn file-name-base
    "strips numbers and file ext from string"
    [file-str]
    (clojure.string/replace file-str #"-?[0-9]*\..*" ""))

(defn files-set
    "gets a set of unique base file names"
    [file-list]
    (set (map file-name-base file-list)))

(defn get-file-versions
    "returns a lazy seq of files in folder whose names start with input string"
    [file-str folder]
    ; (do (println (str "get-file-versions " file-str " " folder))
    (filter #(clojure.string/starts-with? % file-str) (archives-in-dir folder)))

(defn better-compare
    "compare str lengths, then values, so that abc-12 > abc-2. returns the greater str"
    ; consider stripping out "-" if naming system is not consistent
    [a b]
    (if (= (count a) (count b) )
        (if (> (compare a b) 0)
            a
            b)
        (if (> (count a) (count b))
            a
            b)))

(defn get-latest-version
    "get the latest version of file in folder. Not currently used"
    [filestr folder]
        (reduce better-compare (get-file-versions filestr folder)))

(defn glv-from-coll
    [coll]
    (first (filter #(.exists (io/file (str dl-directory %))) (reverse coll))))

(defn make-dir
    "make directory if it does not exist yet"
    [dir-name]
    (#(if (not (and (.exists (io/file %)) (.isDirectory (io/file %))))
        (.mkdir (io/file %))
    ) (file-name-base dir-name)))

(defn extract
    "supports .tar, .tar.gz, .gz, .zip; returns target dir, which is used in recursion."
    ; can't do .rar yet
    [archive from to]
    (cond
        (clojure.string/ends-with? archive ".zip")
            (fsc/unzip (str from archive) (str to (file-name-base archive) "/"))
        (clojure.string/ends-with? archive ".rar")
            ; (fsc/unzip (str from archive) to)
            (println ".rar")
        (clojure.string/ends-with? archive ".tar")
            (do (fsc/untar (str from archive) (str to (file-name-base archive) "/"))
                (str to (file-name-base archive)))
        (clojure.string/ends-with? archive ".tar.gz")
            (do (fsc/gunzip (str from archive) (str to (file-name-base archive) ".tar"))
                (fsc/untar (str to (file-name-base archive) ".tar") (str to (file-name-base archive) "/"))
                (io/delete-file (str to (file-name-base archive) ".tar"))
                (str to (file-name-base archive)))
        (clojure.string/ends-with? archive ".gz")
            (do (fsc/gunzip (str from archive) (str to (file-name-base archive)))
                (str to (file-name-base archive)))
        :else (println "nothing to extract")))

(defn extract-recursive
    ([archive from to]
    (extract-recursive (extract archive from to)))
    ([dir]
    (if (archives-in-dir dir)
        (map #(extract-recursive % dir dir) (archives-in-dir dir)))))

(defn files
    "assumes input is a list of lists"
    [coll & {:keys [from to] :or {from "" to ""}}]
    (->> coll
        (map #(glv-from-coll %))
        (map #(extract-recursive % from to))
        (doall )))


    ; (do
    ; (extract-recursive (glv-from-coll (filter #(not (coll? %)) args)) dl-directory target-directory)
    ; (if (filter coll? args) (map files (filter coll? args)))))




;         (if (archives-in-dir (extract (glv-from-coll arg) dl-directory target-directory))
;             (files ...))))

;         (map (if (coll? %)
;             )
;         (if (.exists (io/file (str dl-directory arg)))
;             (extract arg dl-directory target-directory))
;         )

; (extract (first (take-while #(.exists (io/file (str dl-directory %))) arg)) dl-directory target-directory)


    ; check if input is a file or a list
    ; if file; extract

    ; if list; check args of list
    ; if list, cond if last file exists, extract


(defn -main
    "Extract some-file.tar.gz to ."
    [& {:keys [from to] :or {from dl-directory to target-directory}}]
    ; the keys might have an impact on performance
    ; (println (str "   " from "   " to ))

    (if (> (count (archives-in-dir from)) 0)
        (->> (files-set (archives-in-dir from))
            (map #(get-latest-version % from))
            ; (map #(extract % from (str to (file-name-base %))))
            (map #(extract % from to ))
            (map #(-main :from (str % "/") :to (str % "/")))
            (doall ))))

    ; (if (> (count (archives-in-dir from)) 0)
    ;     (doall
    ;         (map (fn [o] (-main :from (str o "/") :to (str o "/")) )
    ;             (map (fn [n] (extract n from (str to (file-name-base n))))
    ;                 (map (fn [m] (get-latest-version m from)) (files-set (archives-in-dir from)) )
    ;             )
    ;         )
    ;     )
    ; )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; (defn find-files
;     "gets a set of unique base file names"
;     [file-list]
;     (for [x (map #(if (.exists (io/file %)))) y [".tar" ".gz" ".zip" ".rar"] :when (clojure.string/ends-with? x y)] x)
;     (map #(if (.exists (io/file %))
;         ([] into %)
;         (println (str % " not found"))
;         )
;     )
;     ; (set (map file-name-base file-list))
; )

; (defn alt-main
;     "Extract some-file.tar.gz to ."
;     [& files]

;     (if (> (count (archives-in-dir from)))
;         (map (fn [o] (-main :from (str o "/") :to (str o "/")) :files files)
;             (map (fn [n] (extract n from (str to (file-name-base n))))
;                 (map (fn [m] (get-latest-version m)) (find-files [files])
;             )
;         )
;     )
; )

; Script should warn if a file is missing, and also which files were extracted
; Script should optionally delete the source data