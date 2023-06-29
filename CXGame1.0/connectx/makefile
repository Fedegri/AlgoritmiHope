JVMFLAGS = -cp build
JCFLAGS = -cp src -d build
JC = javac
JVM= java 
JVMFLAGS = -cp build
PKG=connectx
TEST=CXPlayerTester
MAIN=$(PKG).CXGame

M:=7
N:=7
X:=4
REPS:=2 # game to play
TIME:=10
BEST:=Morales.Morales
OLD:=L1.L1

.SUFFIXES: .java .class
.PHONY: build

best: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(X) connectx.$(BEST)

a: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(X) connectx.$(BEST) connectx.$(OLD) -r $(REPS)

a2: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(X) connectx.$(OLD) connectx.$(BEST) -r $(REPS)

v: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(X) connectx.$(BEST) connectx.$(OLD) -v -t $(TIME) -r $(REPS)

v2: build
	$(JVM) $(JVMFLAGS) $(PKG).$(TEST) $(M) $(N) $(X) connectx.$(OLD) connectx.$(BEST) -v -t $(TIME) -r $(REPS)

pp: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(X) connectx.$(BEST) connectx.$(OLD)

pp2: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(X) connectx.$(OLD) connectx.$(BEST) 

build: 
	mkdir -p build
	$(JC) $(JCFLAGS) *.java */*.java

clean:
	rm -rf build