package me.joeycumines.javapromises.v1.perf.mather;

import java.util.*;
import java.util.function.Function;

/**
 * It's a string parser that does math and shit, it's for benchmark request - response style.
 */
public class Mather {
    private static Mather globalInstance;

    private final Random rand;

    private Mather() {
        this.rand = new Random();
    }

    public Map.Entry<String, Double> genRandomEquation1(int multiplier) {
        double value = 0;
        StringBuilder equation = new StringBuilder("0");

        for (int x = 0; x < multiplier; x++) {
            double a = this.genDouble(0, 999);
            double b = this.genDouble(0, 999);

            value += a - b;
            equation.append(" + ").append(a).append(" - ").append(b);
        }

        return new AbstractMap.SimpleImmutableEntry<>(equation.toString(), value);
    }

    public double genDouble(int lowerInclusive, int upperInclusive) {
        return (double) (this.rand.nextInt((upperInclusive - lowerInclusive) + 1) + lowerInclusive);
    }

    /**
     * http://stackoverflow.com/a/26227947 this thing is excellent
     */
    public double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

    public static Mather getInstance() {
        // double checked locking
        if (null == globalInstance) {
            synchronized (Mather.class) {
                if (null == globalInstance) {
                    globalInstance = new Mather();
                }
            }
        }

        return globalInstance;
    }
}
