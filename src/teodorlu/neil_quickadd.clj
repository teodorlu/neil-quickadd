(ns teodorlu.neil-quickadd
  (:require
   [babashka.cli :as cli]
   [babashka.fs :as fs]
   [babashka.process :as process :refer [shell]]
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]
   [clojure.set :as set]
   [clojure.string :as str]))

;; temporary workaround for https://github.com/teodorlu/neil-quickadd/issues/1 and https://github.com/babashka/fs/issues/89
(require '[babashka.fs] :reload)

(comment
  (require '[teodorlu.some-lib] :reload))

;; Rationale
;;
;; `neil-quickadd` gets you started with your new Clojure project faster.
;; Add the dependencies you usually use in seconds!
;;
;; There are two important commands:
;;
;;    neil-quickadd scan DIR   ; Traverses DIR to index all your dependencies
;;    neil-quickadd            ; from your quickadd index, select a dep with
;;                              `fzf` and add it with `neil dep add`
;;
;; If you want to script around `neil-quickadd`, also consider:
;;
;;    neil-quickadd libs          ; list indexed libs

(defn ^:private safely-slurp-edn [path orelse]
  (try (edn/read-string (slurp path))
       (catch Exception _ orelse)))

(defn ^:private scan-deps-files
  ([path] (scan-deps-files path nil))
  ([path {:keys [max-depth]}]
   (let [deps-files (map str (concat
                              (fs/glob path "deps.edn")
                              (fs/glob path "**/deps.edn" (when max-depth {:max-depth max-depth}))))
         safe-read-deps (fn [deps-file]
                          (or (seq (keys (:deps (safely-slurp-edn deps-file {}))))
                              (list)))]
     (->> (mapcat safe-read-deps deps-files)
          (into #{})
          sort))))

(defn ^:private neil-quickadd-share-path []
  (fs/expand-home "~/.local/share/neil-quickadd"))

(defn ^:private neil-quickadd-ensure-share-path! []
  (fs/create-dirs (neil-quickadd-share-path)))

(defn ^:private index-file-path []
  (str (neil-quickadd-share-path) "/index.edn"))

(defn ^:private update-index! [k f & args]
  (let [m (safely-slurp-edn (index-file-path) {})
        m (if (map? m) m {})
        m (into (sorted-map) m)]
    (neil-quickadd-ensure-share-path!)
    (spit (index-file-path)
          (with-out-str
            (pprint (apply update m k f args))))))

(defn ^:private blacklist-file-path []
  (str (neil-quickadd-share-path) "/blacklist.edn"))

(defn ^:private blacklist-add! [& libs]
  (let [m (safely-slurp-edn (blacklist-file-path) {})
        m (if (map? m) m {})
        blacklist (get m :blacklist)
        blacklist (if (set? blacklist) blacklist #{})]
    (neil-quickadd-ensure-share-path!)
    (spit (blacklist-file-path)
          (prn-str
           (assoc m :blacklist
                  (into blacklist libs))))))

(defn quickadd-scan [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage:

  neil-quickadd scan [OPTS...]
  neil-quickadd scan DIR [OPTS...]

Traverses DIR for dependencies. DIR defaults to the current directory.
Dependencies are added to the neil-quickadd index.

Allowed OPTS:

  --maxdepth N     ; Limits the traversal to N layers down from DIR.
"))
    (System/exit 0))
  (let [root-dir (:path opts ".")
        root-dir (-> root-dir fs/expand-home fs/absolutize fs/canonicalize str)
        {:keys [max-depth]} opts
        all-deps (scan-deps-files root-dir (when max-depth {:max-depth max-depth}))]
    (update-index! root-dir (fn [_] all-deps))))

(defn quickadd-libs-raw* []
  (into #{} (seq (apply concat (vals (safely-slurp-edn (index-file-path) {}))))))

(defn quickadd-blacklist* []
  (into #{} (:blacklist (safely-slurp-edn (blacklist-file-path) {}))))

(defn quickadd-libs* []
  (set/difference (quickadd-libs-raw*) (quickadd-blacklist*)))

(comment
  (count
   (quickadd-libs*))

  (keys
   (safely-slurp-edn (index-file-path) {}))

  ,)

(defn quickadd-libs [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage: neil-quickadd libs

List indexed libraries.
"))
    (System/exit 0))
  (if-let [libs (sort (quickadd-libs*))]
    (println (str/join "\n" libs))
    (do (println "No libs indexed")
        (println "Please use `neil-quickadd scan` to populate the index")
        (System/exit 1))))

(defn quickadd-blacklist-list
  "Print the blacklist"
  [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage: neil-quickadd blacklist-list

List blacklisted libraries.
"))
    (System/exit 0))
  (let [libs (sort (quickadd-blacklist*))]
    (if (seq libs)
      (println (str/join "\n" libs))
      (do (println "No libs blacklisted")
          (println "Please run `neil-quickadd -h` to learn how to blacklist a lib.")
          (System/exit 1)))))

(defn quickadd-clear-index [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage: neil-quickadd clear-index

Deletes the index of scanned libraries.
"))
    (System/exit 0))
  (fs/delete-if-exists (index-file-path))
  nil)

(defn quickadd-index-path [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage: neil-quickadd-index-path

Prints the location of the index file path.
"))
    (System/exit 0))
  (println (index-file-path)))

(defn quickadd-blacklist
  "Interactively blacklist a library."
  [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage: neil-quickadd blacklist

Select a library to be added to the blacklist. Blacklisted libaries are ignored
when running neil-quickadd.
"))
    (System/exit 0))
  (loop []
    (if-let [libs (sort (quickadd-libs*))]
      (let [fzf-result (process/shell {:out :string :in (str/join "\n" (into [":quit"] libs))} "fzf")]
        (when (not= 0 (:exit fzf-result))
          ;; If FZF terminates, we terminate.
          (System/exit 0))
        (let [selected (str/trim (:out fzf-result))]
          (when (= ":quit" selected)
            ;; Also provide a "non crashing" exit option.
            (System/exit 0))

          ;; FIXME
          (blacklist-add! (symbol selected))
          (recur)))
      (do (println "No libs indexed")
          (println "Please use `neil-quickadd scan` to populate the index")
          (System/exit 1)))))

(defn quickadd-blacklist-lib [{:keys [opts]}]
  (when (or (:h opts) (:help opts) (not (:lib opts)))
    (println (str/trim "
Usage: neil-quickadd blacklist-lib LIB

Select a library to be added to the blacklist. Blacklisted libaries are ignored
when running neil-quickadd.
"))
    (System/exit 0))
  (blacklist-add! (symbol (:lib opts))))

(declare print-subcommands)

(defn ^:private neil-dep-versions [lib]
  (let [process-result (shell {:out :string :continue true}
                              "neil" "dep" "versions" lib)]
    (when (= 0 (:exit process-result))
      (:out process-result))))

(defn ^:private neil-dep-add [& args]
  (apply shell "neil" "dep" "add" args))

(defn quickadd [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (print-subcommands {})
    (System/exit 0))
  (if-let [libs (sort (quickadd-libs*))]
    (loop []
      (let [fzf-result (shell {:out :string
                               :continue true
                               :in (str/join "\n" (into [":quit"] libs))}
                              "fzf")]
        (when (not= 0 (:exit fzf-result))
          ;; If FZF terminates, we terminate.
          (System/exit 0))
        (let [selected (str/trim (:out fzf-result))]
          (when (= ":quit" selected)
            ;; Also provide a "non crashing" exit option.
            (System/exit 0))
          (if (:select-version opts)
            (let [versions-string (neil-dep-versions selected)
                  selected-process-result (shell {:out :string :continue true :in versions-string}
                                                 "fzf")]
              (when (= 0 (:exit selected-process-result))
                (let [selected-tokenized (process/tokenize (:out selected-process-result))]
                  (prn (concat ["neil" "dep" "add"]
                               selected-tokenized))
                  (apply neil-dep-add selected-tokenized)
                 (recur))))
            (do
              (prn ["neil" "dep" "add" selected])
              (neil-dep-add selected)
              (recur)))
          )))
    (do (println "No libs indexed")
        (println "Please use `neil-quickadd scan` to populate the index")
        (System/exit 1))))

(declare dispatch-table)

(defn print-subcommands [{}]
  (println (str/trim "
Usage: neil-quickadd [COMMAND] [OPTION...]

Available commands:

  neil-quickadd                    ; Add a dependency with FZF
  neil-quickadd blacklist          ; Blacklist a library with FZF
  neil-quickadd blacklist-lib LIB  ; Blacklist a library with CLI
  neil-quickadd blacklist-list     ; Print blacklisted libraries
  neil-quickadd clear-index        ; Remove the index
  neil-quickadd index-path         ; Show the location of the index file
  neil-quickadd help               ; Print subcommands
  neil-quickadd libs               ; Show the index
  neil-quickadd scan               ; Scan a folder for dependencies

Available options:

  --select-version             ; Ask the user to select versions, instead of deferring to neil defaults
")))

(def dispatch-table
  [
   {:cmds ["clear-index"]    :fn quickadd-clear-index}
   {:cmds ["index-path"]     :fn quickadd-index-path}
   {:cmds ["help"]           :fn print-subcommands }
   {:cmds ["libs"]           :fn quickadd-libs     }
   {:cmds ["scan"]           :fn quickadd-scan          :args->opts [:path]}
   {:cmds ["blacklist"]      :fn quickadd-blacklist}
   {:cmds ["blacklist-lib"]  :fn quickadd-blacklist-lib :args->opts [:lib]}
   {:cmds ["blacklist-list"] :fn quickadd-blacklist-list}
   {:cmds []                 :fn quickadd          }
   ])

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
