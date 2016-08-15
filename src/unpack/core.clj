; skip download step?
; http://stackoverflow.com/questions/32742744/how-to-download-a-file-and-unzip-it-from-memory-in-clojure
(ns unpack.core
  (:gen-class))

(require '[me.raynes.fs.compression :as fsc] ; https://github.com/Raynes/fs/blob/master/src/me/raynes/fs/compression.clj
         '[clojure.java.io :as io]
         ; '[com.github.junrar.junrar :as junrar]
         ; '[org.clojars.bonega.java-unrar :as unrar]
         )
; (:import org.clojars.bonega.java-unrar)
; (import com.github.junrar)


(def dl-directory "zipped-files/")
(def target-directory "zipped-files/")

(defn archives-in-dir
    "gets the names of archive files in target dir (returns lazy seq)"
    [dir]
    (for [x (seq (.list (io/file dir))) y [".tar" ".gz" ".zip" ".rar"] :when (clojure.string/ends-with? x y)] x)
)

(defn file-name-base
    "strips numbers and file ext from string"
    [file-str]
    (clojure.string/replace file-str #"-?[0-9]*\..*" "")
)

(defn files-set
    "gets a set of unique base file names"
    [file-list]
    (set (map file-name-base file-list))
)

(defn get-file-versions
    "returns a lazy seq of files in folder whose names start with input string"
    [file-str folder]
    ; (do (println (str "get-file-versions " file-str " " folder))
    (filter #(clojure.string/starts-with? % file-str) (archives-in-dir folder) )
    ; )
)

(defn better-compare
    "compare str lengths, then values, so that abc-12 > abc-2. returns the greater str"
    ; consider stripping out "-" if naming system is not consistent
    [a b]
    (if (= (count a) (count b) )
        (if (> (compare a b) 0)
            a
            b
        )
        (if (> (count a) (count b))
            a
            b
        )
    )
)

(defn get-latest-version
    "get the latest version of file in folder. Not currently used"
    [filestr folder]
    ; (do (println (str "get-latest-version " filestr " " folder))
        (reduce better-compare (get-file-versions filestr folder))
    ; )
)

(defn make-dir
    "make directory if it does not exist yet"
    [dir-name]
    (#(if (not (and (.exists (io/file %)) (.isDirectory (io/file %))))
        (.mkdir (io/file %))
    ) (file-name-base dir-name))
)

(defn extract
    "supports .tar, .tar.gz, .gz, .zip; returns target dir, which is used in recursion."
    ; can't do .rar yet
    [archive-name from to]
    (if (.exists (io/file (str from archive-name)))
        (if (clojure.string/ends-with? archive-name ".zip")
            (fsc/unzip (str from archive-name) to)
            (if (clojure.string/ends-with? archive-name ".tar.gz")
                (do
                    (println (str from archive-name))
                    (println (fsc/gunzip (str from archive-name) "tmp/"))
                    (println (str "tmp/" archive-name))
                    (println (fsc/untar (str "tmp/" archive-name) to))
                )
                (if (clojure.string/ends-with? archive-name ".gz")
                    (fsc/gunzip (str from archive-name) to)
                    (if (clojure.string/ends-with? archive-name ".tar")
                        (fsc/untar (str from archive-name) to)
                        (if (clojure.string/ends-with? archive-name ".rar")
                            (println ".rar")
                            (println "nothing to extract")
                        )
                    )
                )
            )
        )
    (println (str from archive-name "does not exist"))
    )
)

(defn -main
    "Extract some-file.tar.gz to ."
    [& {:keys [from to] :or {from dl-directory to target-directory}}]
    ; the keys might have an impact on performance
    ; (println (str "   " from "   " to ))

    (if (> (count (archives-in-dir from)))
        (doall
            (map (fn [o] (-main :from (str o "/") :to (str o "/")) )
                (map (fn [n] (extract n from (str to (file-name-base n))))
                    (map (fn [m] (get-latest-version m from)) (files-set (archives-in-dir from)) )
                )
            )
        )
    )
)

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