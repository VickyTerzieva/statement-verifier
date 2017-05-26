/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.org.nvl.core.rpn.verifier;

import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.variable.Type;

import java.util.Stack;
import java.util.StringTokenizer;

import static src.org.nvl.MessageConstants.*;

/**
 * @author niki
 */
public class NumberRpnVerifier extends AbstractRpnVerifier {

    @Override
    //creates the Reverse Polish Notation
    public String createRpn(String input) {
        StringBuilder result = new StringBuilder();   //builder for the final result (RPN)
        Stack<Character> operationStack = new Stack<>();  //sttack for the operation
        char[] charInput = input.toCharArray();  //char array for the input
        int i = 0;
        if (charInput[i] == '-' || charInput[i] == '+') {    //if the expresion begins with - or + (e.g. -52+3..)
            result.append(charInput[i]);
            i++;
        } //end of if
        while (i < charInput.length) {   //iterrate through the input
            switch (charInput[i]) {
                case '-':
                case '+':
                    while (!operationStack.empty() && operationStack.peek() != '(' && operationStack.peek() != ')') {   //if the previous operations in the stack have higher priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    result.append(' ');
                    operationStack.push(charInput[i]);
                    break;
                case '*':
                case '/':
                    while (!operationStack.empty() && (operationStack.peek() == '*' || operationStack.peek() == '/')) {   //if the previous operations in the stack have higher priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    result.append(' ');
                    operationStack.push(charInput[i]);
                    break;
                case '(':
                    operationStack.push(charInput[i]);
                    break;
                case ' ':
                    break;
                case ')':
                    while (!operationStack.empty() && operationStack.peek() != '(') {   // pop everything from stack to the result until we get to the '('
                        result.append(' ').append(operationStack.pop());
                    }
                    if (!operationStack.empty()) {    //remove the '('
                        operationStack.pop();
                    }
                    break;
                case '^':
                case '!':
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                default:
                    result.append(charInput[i]);    // we have a digit
                    break;
            }  //end of switch
            i++;
        }  //end of while
        while (!operationStack.isEmpty()) {  //pop every operation from the stack to the result
            result.append(' ').append(operationStack.pop());
        }  //end of while
        return result.toString();  //return resulted RPN
    }  //end of create RPN

    @Override
    public String calculateRpn(String input) {
        StringTokenizer tokens = new StringTokenizer(input);  //tokenize the input by ' '
        Stack<Double> stack = new Stack<>();  //stack for the numbers
        while (tokens.hasMoreTokens()) {   //while we have more tokens
            String current = tokens.nextToken();   //get current token
            if (Type.isNumber(current)) {   //if the token is a number, push it in the stack
                stack.push(Double.parseDouble(current));
            } else {    //else it is operation
                if(stack.empty()) {
                    throw new RuntimeException(EMPTY_STACK_MESSAGE);
                }
                Double right = stack.pop();  //get the right number
                if(stack.empty()) {
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                }
                Double left = stack.pop();   //get the left
                switch (current) {    //current is an operation, so wi push the resulted number in the stack
                    case "+":
                        stack.push(left + right);
                        break;
                    case "-":
                        if(left - right >= 0) {
                            stack.push(left - right);
                        } else {
                            throw new RuntimeException(NEGATIVE_NUMBERS_MESSAGE);
                        }
                        break;
                    case "*":
                        stack.push(left * right);
                        break;
                    case "/":
                        if(right == 0) {
                            throw new RuntimeException(ZERO_DIVISION_MESSAGE);
                        }
                        stack.push(left / right);
                        break;
                } //end of switch
            }  // end of if/else
        }     //end of while
        return String.valueOf(stack.pop().intValue());   //the result is in the stack(last element)
    }   //end of calculate RPN
}
