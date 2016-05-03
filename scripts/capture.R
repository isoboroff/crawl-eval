#!/usr/bin/env Rscript
library(Rcapture)
args <- commandArgs()

captures <- read.table(args[2], header=TRUE)
summary(captures)
caps <- captures[2:5]
closedp.t(caps)
colSums(caps) / closedpCI.t(caps, m="Mth")$n
colSums(caps) / closedpCI.t(caps, m="Mth")$results[1]