ANTLR := /Users/nargizi/Desktop/Uni/Compilers/ANTLR-4.9.3/antlr-4.9.3-complete.jar
GRAMMAR := Tiger.g4
MAIN_CLASS_NAME := Main
JAR_DIR := cs8803_bin
BUILD_DIR := build
COMPILER_JAR := tigerc.jar

ANTLR_JAVA_FILES := \
src/TigerBaseListener.java \
src/TigerBaseVisitor.java \
src/TigerLexer.java \
src/TigerListener.java \
src/TigerParser.java \
src/TigerVisitor.java

ANTLR_FILES := \
src/Tiger.interp \
src/Tiger.tokens \
src/TigerLexer.interp \
src/TigerLexer.tokens \
$(ANTLR_JAVA_FILES)

ANTLR_LIBS := \
$(BUILD_DIR)/javax \
$(BUILD_DIR)/org

SOURCES := \
src/GraphVizBuilder.java \
src/ParseTreeToGraphViz.java \
src/Main.java \
src/TokensLogger.java

.PHONY :
all: $(COMPILER_JAR)

$(COMPILER_JAR): $(SOURCES) $(ANTLR_JAVA_FILES) $(ANTLR_LIBS)
	@mkdir -p $(BUILD_DIR) $(JAR_DIR)
	@javac -d $(BUILD_DIR) -cp "src:$(ANTLR)" $(SOURCES) $(ANTLR_JAVA_FILES)
	@cd $(BUILD_DIR) && jar cfe ../$(JAR_DIR)/$(COMPILER_JAR) \
	$(MAIN_CLASS_NAME) *.class  org  javax && cd ..

$(ANTLR_JAVA_FILES): $(GRAMMAR)
	@java -jar $(ANTLR) -o src/ -visitor $(GRAMMAR)

$(ANTLR_LIBS):
	@mkdir -p  $(BUILD_DIR)
	@cd $(BUILD_DIR) &&  jar xf $(ANTLR) && cd .. @rm -rf  $(BUILD_DIR)/META-INF

.PHONY:
clean:
	@rm -f $(JAR_DIR)/$(COMPILER_JAR) $(ANTLR_FILES) $(BUILD_DIR)/*.class
	@rm -rf $(ANTLR_LIBS)

