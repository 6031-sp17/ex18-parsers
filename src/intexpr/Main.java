package intexpr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import lib6005.parser.ParseTree;
import lib6005.parser.Parser;
import lib6005.parser.UnableToParseException;
import lib6005.parser.Visualizer;

public class Main {

    private static final File grammarFile = new File("IntegerExpression.g");

    // the nonterminals of the grammar
    private enum IntegerGrammar {
        ROOT, SUM, PRIMARY, NUMBER, WHITESPACE,
    };

    /**
     * Main method. Makes an IntegerExpression parser, uses it to parse an
     * example expression, then evaluates the expression.
     * 
     * @param args command line arguments, not used
     * @throws UnableToParseException if example expression can't be parsed
     */
    public static void main(final String[] args) throws UnableToParseException {
        // make a parser from the grammar
        final Parser<IntegerGrammar> parser = makeParser(grammarFile);

        // an example expression
        final String input = "54+(2+ 89)";
        System.out.println(input);

        // parse the example into a parse tree
        final ParseTree<IntegerGrammar> parseTree = parser.parse(input);
        System.out.println("parse tree " + parseTree);

        // display the parse tree in a web browser, for debugging
        Visualizer.showInBrowser(parseTree);

        // make an AST from the parse tree
        final IntegerExpression expression = makeAbstractSyntaxTree(parseTree);
        System.out.println("AST " + expression);

        // evaluate the expression
        final int value = expression.value();
        System.out.println("value " + value);
        
    }

    /**
     * Compile the grammar into a parser.
     * 
     * @param grammar
     *            file containing the grammar
     * @return parser for the grammar
     * @throws RuntimeException
     *             if grammar file can't be read or has syntax errors
     */
    private static Parser<IntegerGrammar> makeParser(final File grammar) {
        try {

            return Parser.compile(grammar, IntegerGrammar.ROOT);

            // translate these checked exceptions into unchecked
            // RuntimeExceptions,
            // because these failures indicate internal bugs rather than client
            // errors
        } catch (IOException e) {
            throw new RuntimeException("can't read the grammar file", e);
        } catch (UnableToParseException e) {
            throw new RuntimeException("the grammar has a syntax error", e);
        }
    }

    /**
     * Convert a parse tree into an abstract syntax tree.
     * 
     * @param parseTree constructed according to the grammar in IntegerExpression.g
     * @return abstract syntax tree corresponding to parseTree
     */
    private static IntegerExpression makeAbstractSyntaxTree(final ParseTree<IntegerGrammar> parseTree) {
        switch (parseTree.name()) {
        case ROOT: // root ::= sum;
            {
                final ParseTree<IntegerGrammar> child = parseTree.children().get(0);
                return makeAbstractSyntaxTree(child);
            }

        case SUM: // sum ::= primary ('+' primary)*;
            {
                final List<ParseTree<IntegerGrammar>> children = parseTree.children();
                IntegerExpression expression = makeAbstractSyntaxTree(children.get(0));
                for (int i = 1; i < children.size(); ++i) {
                    expression = new Plus(expression, makeAbstractSyntaxTree(children.get(i)));
                }
                return expression;
            }

        case PRIMARY: // primary ::= number | '(' sum ')';
            {
                final ParseTree<IntegerGrammar> child = parseTree.children().get(0);
                // check which alternative (number or sum) was actually matched
                switch (child.name()) {
                case NUMBER:
                    return makeAbstractSyntaxTree(child);
                case SUM:
                    return makeAbstractSyntaxTree(child); // in this case, we do the
                                                          // same thing either way
                default:
                    throw new AssertionError("should never get here");
                }
            }

        case NUMBER: // number ::= [0-9]+;
            {
                final int n = Integer.parseInt(parseTree.text());
                return new Number(n);
            }
            
        default:
            throw new AssertionError("should never get here");
        }

    }

}
