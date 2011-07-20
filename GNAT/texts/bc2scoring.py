"""

This script is meant to score entries for the normalized gene list task for BioCreAtIvE 2 (2006-2007).

It is called:  python bc2scoring.py 'goldstandard' 'testfile' [recall|precision|all]

'goldstandard' is the name of the goldstandard file

'testfile is the name of the genelist file to be tested

The optional arguments (recall, precision or all) will output the errors in a table form at the end of the score information.  This may help in analyzing system performance.  Recall error reports misses; precision error reports false positives, and all reports all the results including correct matches.

All output is to standard out, except error remarks which are to standard error.

The file formats are tab delimited with one pubmed/gene/excerpt entry per line with no header or line numbers.

goldstandard:
PMID   GeneID   Exceprt1   Excerpt2   Excerpt3 ...

testfile:
PMID   GeneID   Excerpt

Note that only one text excerpt is required or examined (the first) for each line of the testfile.  The best match to an excerpt in then counted.  The output is as follows.

True Positive:
False Positive:
False Negative:
Precision:
Recall:
Thresholded TP:
Thresholded FP:
Thresholded FN:
Thresholded P:
Thresholded R:
Gold File Errors:
Test File Errors:


The similarity score is 1 - ArgMin(EditDistance/MeanLength, 1) 

The threshold is that the similarity score must be greater than 0.55

The evaluation output is of the form:

PMID GeneID   TextExcerpt EditScore GSExcerprt1  GSExcerpt2 ...



- Alex Morgan, amorgan@mitre.org/alexmo@stanford.edu

copyright 18 May 2006, MITRE Corporation

"""

import string, sys, os
from sets import Set

THRESHOLD = .55

reportF = None  # recall (misses), precision (false pos), all



# Check arguments, define goldfile testfile and reportF

if len(sys.argv) < 3 or len(sys.argv) > 4:
    print >>sys.stderr, "Error with input format, should be of the form python bc2scoring.py 'goldstandard' 'testfile' [recall|precision|all]"
    sys.exit(-1)

goldfile = sys.argv[1]
testfile = sys.argv[2]

if (not os.access(goldfile, os.R_OK)) or (not os.access(testfile, os.R_OK)) :
    print >>sys.stderr, "Error with input files, check permissions and existence;  python bc2scoring.py 'goldstandard' 'testfile' [recall|precision|all]"
    sys.exit(-1)

if len(sys.argv) == 4:
    reportF = sys.argv[3].lower()
    if (reportF != 'precision') and  (reportF != 'recall') and  (reportF != 'all'):
        print >>sys.stderr, "Error with output request '%s', not producing any extra reporting; should be one of  [recall|precision|all]" % reportF
        reportF = None

testFileErrors = 0
goldFileErrors = 0
truePositive = 0
falsePositive = 0
falseNegative = 0
threshTruePositive = 0
threshFalsePositive = 0
threshFalseNegative = 0
missingExcerpts = 0

def distance(a,b):
    "Calculates the Levenshtein distance between a and b.  Taken from wikisource http://en.wikisource.org/wiki/Levenshtein_distance#Python attributed to Magnus Lie Hetland "
    n, m = len(a), len(b)
    if n > m:
        # Make sure n <= m, to use O(min(n,m)) space
        a,b = b,a
        n,m = m,n
        
    current = range(n+1)
    for i in range(1,m+1):
        previous, current = current, [i]+[0]*n
        for j in range(1,n+1):
            add, delete = previous[j]+1, current[j-1]+1
            change = previous[j-1]
            if a[j-1] != b[i-1]:
                change = change + 1
            current[j] = min(add, delete, change)
            
    return current[n]

def simScore(excerptL, testtext):
    if not excerptL or not testtext: return 0
    simf = lambda x: (1.0 - distance(x, testtext)*2.0/(len(x)+len(testtext)))
    mv = max(map(simf, excerptL))
    return max([mv, 0])

gfo = open(goldfile)
goldD = {}

for line in gfo.xreadlines():
    line = line.strip()
    lineL = line.split("\t")
    if len(lineL) < 3:
        print >>sys.stderr, "Error parsing GoldFile, ignoring line: " + repr(line)
        goldFileErrors += 1
        continue
    pmid = lineL[0]
    gnid = lineL[1]
    excerptL = lineL[2:]
    #print "%s %s %s " % (pmid, gnid, repr(excerptL))
    try:
        pmid = string.atoi(pmid)
        gnid = string.atoi(gnid)
    except:
        print >>sys.stderr, "Error parsing GoldFile, incorrect identifier, ignoring line: " + repr(line)
        goldFileErrors += 1
        continue
    dkey = "%d:%d" % (pmid, gnid)
    goldD[dkey] = excerptL

#sys.exit()

tfo = open(testfile)
testD = {}
   
for line in tfo.xreadlines():
    line = line.strip()
    lineL = line.split("\t")
    if len(lineL) < 3:
        print >>sys.stderr, "Error parsing TestFile, perhaps missing excerpt, line: " + repr(line)
        missingExcerpts += 1
        if len(lineL) < 2:
            continue
        excerpt = ""
    else:
        excerpt = lineL[2]
    pmid = lineL[0]
    gnid = lineL[1]
    try:
        pmid = string.atoi(pmid)
        gnid = string.atoi(gnid)
    except:
        print >>sys.stderr, "Error parsing TestFile, incorrect identifier, ignoring line: " + repr(line)
        testFileErrors += 1
        continue
    dkey = "%d:%d" % (pmid, gnid)
    testD[dkey] = excerpt

sys.stderr.flush()

def keyCompare(a,b):
    ap, ag = map(string.atoi,a.split(":"))
    bp, bg = map(string.atoi,b.split(":"))
    if ap != bp: return cmp(ap,bp)
    else: return cmp(ag,bg)

    
idS = Set(testD.keys())
idS.update(goldD.keys())

idL = list(idS)
idL.sort(keyCompare)

outlineL = []


for id in idL:
    etype = None
    G = 0
    T = 0
    if goldD.has_key(id):
        G = 1
        excerptL = goldD[id]
    else: excerptL = []
    if testD.has_key(id):
        T = 1
        excerpt = testD[id]
    else: excerpt = ""
    similarity = 0
    if G and T:
        truePositive += 1
        similarity = simScore(excerptL, excerpt)
        if similarity > THRESHOLD:
            threshTruePositive += 1
        else:
            threshFalsePositive += 1
            threshFalseNegative += 1
    if G and not T:
        etype = "recall"
        falseNegative +=1
        threshFalseNegative += 1
    if T and not G:
        etype = "precision"
        falsePositive += 1
        threshFalsePositive += 1
    if reportF:
        if (reportF=="all") or (reportF == etype):
            pmid, gnid = id.split(":")
            line  = "%s\t%s\t%s\t%.3f\t%s" % (pmid, gnid, excerpt, similarity, string.join(excerptL, "\t"))
            outlineL.append(line)

def recallF(TP, FP, FN):
    denom = TP + FN
    if denom == 0:
        return 1
    else: return float(TP)/denom

def precisionF(TP, FP, FN):
    denom = TP + FP
    if denom == 0:
        return 1
    else: return float(TP)/denom


precision = precisionF(truePositive, falsePositive, falseNegative)
recall    = recallF(truePositive, falsePositive, falseNegative)

fmeasure = (2*precision*recall)/(precision+recall)

print """

F-Measure:   %5f
True Positive:   %5d
False Positive:  %5d
False Negative:  %5d
Precision:       %5.3f
Recall:          %5.3f
Thresholded TP:  %5d
Thresholded FP:  %5d
Thresholded FN:  %5d
Thresholded P:   %5.3f
Thresholded R:   %5.3f
Gold File Errors:%5d
Test File Errors:%5d
Missing Excerpts:%5d""" % (fmeasure,truePositive, falsePositive, falseNegative, precisionF(truePositive, falsePositive, falseNegative), recallF(truePositive, falsePositive, falseNegative), threshTruePositive, threshFalsePositive, threshFalseNegative, precisionF(threshTruePositive, threshFalsePositive, threshFalseNegative), recallF(threshTruePositive, threshFalsePositive, threshFalseNegative), goldFileErrors, testFileErrors, missingExcerpts)

if reportF:
    for line in outlineL:
        print line


        


    

        
