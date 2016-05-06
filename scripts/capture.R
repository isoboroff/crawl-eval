#!/usr/bin/env Rscript

library(Rcapture)
args <- commandArgs(TRUE)

captures <- read.table(args[1], header=TRUE)
print(summary(captures))
caps <- captures[2:5]
print(closedp.t(caps))
ci <- closedpCI.t(caps, m="Mth")
print(ci)
print(colSums(caps) / ci$n)
print(colSums(caps) / ci$results[1])

