#!/usr/bin/env Rscript

library(Rcapture)
library(VennDiagram)
args <- commandArgs(TRUE)

draw_venn <- function(captures) {
  diagram <- draw.triple.venn(area1=sum(captures[,2]), 
                              area2=sum(captures[,3]), 
                            area3=sum(captures[,4]), 
                            n12=sum(captures[,2]+captures[,3]==2), 
                            n23=sum(captures[,3]+captures[,4]==2), 
                            n13=sum(captures[,2]+captures[,4]==2), 
                            n123=sum(captures[,2]+captures[,3]+captures[,4]==3), 
                            category=colnames(captures)[2:4], 
                            fill=c("blue", "red", "green"), 
                            lty="blank", 
                            cex=2, 
                            cat.cex=2,
                            cat.col=c("blue", "red", "green"),
                            euler.d=FALSE, scaled=FALSE)
  return(diagram)
}

captures <- read.table(args[1], header=TRUE)
print(summary(captures))
caps <- captures[2:4]
print(closedp.t(caps))
ci <- closedpCI.t(caps, m="Mth")
print(ci)
print(colSums(caps) / ci$n)
print(colSums(caps) / ci$results[1])
vv <- draw_venn(captures)
pdf()
grid.draw(vv)



