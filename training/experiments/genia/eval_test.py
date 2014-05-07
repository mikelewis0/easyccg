import sys
from collections import Counter

rawfile=sys.argv[1]
goldfile=sys.argv[2]
infile=sys.argv[3]


brackets={}
brackets["-LRB-"]="("
brackets["-RRB-"]=")"
brackets["-LSB-"]="["
brackets["-RSB-"]="]"
brackets["-LCB-"]="{"
brackets["-RCB-"]="}"


file=open("/group/corpora/public/ccgbank/data/LEX/CCGbank.02-21.lexicon")

seenw=set()
for line in file:
	word=line.split()[0]
	cat=line.split()[1]
	#cat=rgx.sub("S",cat)
	word=brackets.get(word,word)
	wordcat=word+" "+cat
	if word not in seenw:
		seenw.add(word)

file.close()


stoks={}
snum=0
file=open(rawfile)
for line in file:
	stuff=line.split()
	t=0
	stoks[snum]={}
	for tok in stuff:
		stoks[snum][t]=tok
		t=t+1
	snum=snum+1
file.close()


file=open(goldfile)
snum=0
sentences={}
totalr=Counter()
totr=0
totrCov=0
for line in file:
	stuff=line.split()
	id=stuff[0]
	sentences[snum]=set()
	for dep in stuff[1:]:
		argstype=dep.rstrip("]").split("[")
		args=argstype[0].split("-")
		deptype=argstype[1].lstrip("<").rstrip(">")
		argl=int(args[0])
		argr=int(args[1])
		if argstype[1][0]=="<":
			temp=argr
			argr=argl
			argl=temp
		sentences[snum].add(deptype+" %03d %03d "%(argl,argr))
		#totalr[deptype]=totalr[deptype]+1
		totr=totr+1
	snum=snum+1
file.close()

print snum

stuffcount=Counter()


file=open(infile)
flag=0
snum=0
totc=0
correct=Counter()
corpos=Counter()
scorpos=Counter()
slcorrect=Counter()
srcorrect=Counter()
lrtotc=0
wrong=Counter()
totp=0
totalp=Counter()
totpos=Counter()
stotpos=Counter()
sltotalp=Counter()
srtotalp=Counter()
lrtotp=0
sltotalr=Counter()
srtotalr=Counter()
lrtotr=0
cov=0
deps=set()
sents=0
for line in file:
	stuff=line.split()
	if len(stuff) > 0 and stuff[0]=="<c>":
		t=0
		wpos={}
		for tokcat in stuff[1:]:
			tok=tokcat.split("|")[0]
			pos=tokcat.split("|")[1]
			wpos[t]=pos
			if tok != stoks[snum][t] and "&" not in stoks[snum][t]:
				print snum, t, tok, stoks[snum][t]
				snum=snum+1
			t=t+1
		args=set()
		if deps:
			for dep in sentences[snum]:
				deptype=dep.split()[0]
				argl=int(dep.split()[1])
				argr=int(dep.split()[2])
				args.add(argl)
				args.add(argr)
				wordl=stoks[snum][argl]
				wordr=stoks[snum][argr]
				seenl=(wordl not in seenw)
				seenr=(wordr not in seenw)
				if seenl:
					sltotalr[deptype]=sltotalr[deptype]+1
				if seenr:
					srtotalr[deptype]=srtotalr[deptype]+1
				if seenl and seenr:
					lrtotr=lrtotr+1
				totalr[deptype]=totalr[deptype]+1
				totrCov=totrCov+1
				#if deptype=="nsubj":
				#	totpos[wpos[argl]]=totpos[wpos[argl]]+1
				#	if seenl:
				#		stotpos[wpos[argl]]=stotpos[wpos[argl]]+1
			for depw in deps:
				dep=" ".join(depw.split()[0:3])+" "
				wordl=depw.split()[3]
				wordr=depw.split()[4]
				seenl=(wordl not in seenw)
				seenr=(wordr not in seenw)
				deptype=dep.split()[0]
				argl=int(dep.split()[1])
				argr=int(dep.split()[2])
				if dep in sentences[snum]:
					totc=totc+1
					correct[deptype]=correct[deptype]+1
					if seenl:
						slcorrect[deptype]=slcorrect[deptype]+1
					if seenr:
						srcorrect[deptype]=srcorrect[deptype]+1
					if seenl and seenr:
						lrtotc=lrtotc+1
					if deptype=="nsubj":
						corpos[wpos[argl]]=corpos[wpos[argl]]+1
						if seenl:
							scorpos[wpos[argl]]=scorpos[wpos[argl]]+1
				if argl in args and argr in args:
					totp=totp+1
					totalp[deptype]=totalp[deptype]+1
					if seenl:
						sltotalp[deptype]=sltotalp[deptype]+1
					if seenr:
						srtotalp[deptype]=srtotalp[deptype]+1
					if seenl and seenr:
						lrtotp=lrtotp+1
		if flag==1:
			cov=cov+1
		if len(sentences[snum]) > 0:
			sents=sents+1
		snum=snum+1
		flag=0
		deps=set()
	elif len(line)>0 and line[0]=="(":
		flag=1
		#if stuff[1] in ["_"]:#,"part","num","poss"]:
		#	del stuff[1]
		stuffcount[len(stuff)]=stuffcount[len(stuff)]+1
		if len(stuff) == 3:
			deptype=stuff[0][1:]
			wordl=stuff[1].split("_")[0]
			wordr=stuff[2].split("_")[0]
			if stuff[1].split("_")[1] != "" and stuff[2].split("_")[1] != "":
				argl=int(stuff[1].split("_")[1])
				argr=int(stuff[2].split("_")[1].rstrip(")"))
				deps.add(deptype+" %03d %03d "%(argl,argr)+" "+wordl+" "+wordr)
file.close()


#print snum
print "Coverage: ", sents, snum, float(cov)/float(snum)

precision=float(totc)/float(totp)
recall=float(totc)/float(totrCov)
fscore=2*(precision*recall)/(precision+recall)

print "recall (cov):", totc, totrCov, recall
print "precision (cov):", totp, totc, precision
print "fscore (cov):", fscore
print

precision=float(totc)/float(totp)
recall=float(totc)/float(totr)
fscore=2*(precision*recall)/(precision+recall)

print "recall (all):", totr, totc, recall
print "precision (all):", totp, totc, precision
print "fscore (all):", fscore
print


for deptype, countr in totalr.most_common():
	countp=max(totalp[deptype],1)
	slcountp=max(sltotalp[deptype],1)
	srcountp=max(srtotalp[deptype],1)
	slcountr=max(sltotalr[deptype],1)
	srcountr=max(srtotalr[deptype],1)
	print deptype, countr, correct[deptype], float(correct[deptype])/float(countr), float(correct[deptype])/float(countp), slcountr, float(slcorrect[deptype])/float(slcountr), srcountr, float(srcorrect[deptype])/float(srcountr)
print

for pos, count in totpos.most_common():
	scount=max(1,stotpos[pos])
	print pos, count, scount, float(corpos[pos])/float(count), float(scorpos[pos])/float(scount)


	
