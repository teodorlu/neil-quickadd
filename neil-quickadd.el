;;; neil-quickadd.el -*- lexical-binding: t; -*-
;;
;; Copyright (C) 2023 Teodor Heggelund
;;
;; Author: Teodor Heggelund <teodorlu@teod-t490s>
;; Maintainer: Teodor Heggelund <teodorlu@teod-t490s>
;; Created: January 28, 2023
;; Modified: January 29, 2023
;; Version: 0.0.1
;; Keywords: abbrev bib c calendar comm convenience data docs emulations extensions faces files frames games hardware help hypermedia i18n internal languages lisp local maint mail matching mouse multimedia news outlines processes terminals tex tools unix vc wp
;; Homepage: https://github.com/teodorlu/neil-quickadd
;; Package-Requires: ((emacs "24.3"))
;;
;; This file is not part of GNU Emacs.
;;
;;; Commentary:
;;
;;
;;
;;; Code:

(require 'projectile)
(require 's)

(defun neil-quickadd--binary ()
  "Which neil binary name to use"
  "neil-quickadd")

(defun neil-quickadd ()
  "Add a deps.edn dependency."
  (interactive)
  (let* ((libs (s-lines (s-trim (shell-command-to-string (s-concat (neil-quickadd--binary) " libs")))))
         (selected (completing-read "add lib > " libs)))
    (projectile-run-shell-command-in-root (s-concat "neil dep add " selected))))

(defun neil-quickadd-blacklist ()
  "Blacklist a deps.edn dependency."
  (interactive)
  (let* ((libs (s-lines (s-trim (shell-command-to-string (s-concat (neil-quickadd--binary) " libs")))))
         (selected (completing-read "blacklist lib > " libs)))
    (shell-command-to-string (s-concat (neil-quickadd--binary) " blacklist-lib " selected))))

(defun neil-quickadd-upgrade ()
  "Upgrade deps.edn dependencies."
  (interactive)
  (projectile-run-shell-command-in-root "neil dep upgrade"))

(provide 'neil-quickadd)
;;; neil-quickadd.el ends here
