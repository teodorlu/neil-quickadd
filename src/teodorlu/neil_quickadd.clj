(ns teodorlu.neil-quickadd
  (:require
   [babashka.cli :as cli]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.edn :as edn]
   [clojure.string :as str]))

;; Rationale
;;
;; neil dep add requires me to remember coordinates. I /can/ use Ctrl+R to find stuff I've added previously.
;;
;; neil-quickadd approaches it differently.
;; There are two commands:
;;
;;    neil-quickadd rescan   ; builds the quickadd index from your $SHELL history
;;    neil-quickadd          ; based on your $SHELL history, add a dep, selecting dep with FZF

(defn read-zshhistory []
  (let [lines (->> (fs/expand-home "~/.zsh_history")
                   str
                   slurp
                   str/split-lines)
        neil-lines (filter #(str/includes? % "neil dep add") lines)
        libs (map (fn [raw-line]
                    (first
                     (str/split (str/trim (str/replace raw-line #".*neil dep add" ""))
                                #"\s+")))
                  neil-lines)]
    (->> libs
         (remove #{"" ":h" "-h" "--help" ":help" "--lib" ":lib"})
         (into #{})
         sort)))

(defn read-bashhistory []
  ;; not yet!
  (list))

(defn quickadd-rescan [{}]
  (let [shell (-> (System/getenv "SHELL") (str/split #"/") last)
        history (cond (= shell "zsh") (read-zshhistory)
                      (= shell "bash") (read-bashhistory))]
    (if-let [history (seq history)]
      (do
        (fs/create-dirs (fs/expand-home "~/.local/share/neil-quickadd"))
        (spit (str (fs/expand-home "~/.local/share/neil-quickadd") "/history.edn")
              (pr-str {:history history})))
      (do (println "Unable to rescan history! This might be a bug.")
          (System/exit 1)))))

(defn quickadd [{}]
  ;; TODO validation
  (let [command-history (:history (edn/read-string (slurp (str (fs/expand-home "~/.local/share/neil-quickadd") "/history.edn"))))
        selected (str/trim (:out (process/shell {:out :string :in (str/join "\n" command-history)} "fzf")))]
    (prn ["neil" "dep" "add" selected])
    (process/shell "neil" "dep" "add" selected)
    ))

(declare dispatch-table)

(defn print-subcommands [{}]
  (println "usage: ./play.clj <command>")
  (println "")
  (println "available commands:")
  (doseq [{:keys [cmds]} dispatch-table]
    (println (str "  " (str/join " " cmds)))))

(def dispatch-table
  [{:cmds ["help"] :fn print-subcommands}
   {:cmds ["rescan"] :fn quickadd-rescan}
   {:cmds [] :fn quickadd}])

(defn ensure-env-ok
  "Terminate and give user error if the user needs to install something."
  []
  (when-not (fs/which "fzf")
    (println "neil-quickadd requires fzf.")
    (println "Please install fzf, then call `neil-quickadd` again.")
    (System/exit 1)))

(defn -main [& args]
  (ensure-env-ok)
  (cli/dispatch dispatch-table args))
