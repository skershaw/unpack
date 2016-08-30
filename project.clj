(defproject unpack "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [me.raynes/fs "1.4.6"]
                 [pacl "0.1.2-SNAPSHOT"]
                 [org.clojure/core.async "0.2.385"]
                 [com.github.junrar/junrar "0.7"]]
                 ; Can't find proper syntax to use the following two libraries.
                 ; [com.github.beothorn/junrar "0.6"]
                 ; [org.clojars.bonega/java-unrar "0.5.0"]]
  :main ^:skip-aot unpack.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
