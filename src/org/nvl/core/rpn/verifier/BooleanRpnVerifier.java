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

import static src.org.nvl.MessageConstants.INVALID_INPUT_MESSAGE;

/**
 * @author niki
 */
public class BooleanRpnVerifier extends AbstractRpnVerifier {

    @Override
    public String createRpn(String input) {
        StringBuilder result = new StringBuilder();   //builder for the final result (RPN)
        Stack<String> operationStack = new Stack<>();  //stack for the operation
        char[] charInput = input.toCharArray();  //char array for the input
        int i = 0;
        while (i < charInput.length) {   //iterate through the input
            String sw;
            if (i < charInput.length - 1 && charInput[i] == charInput[i + 1]) {
                sw = new String(charInput, i, 2);
                i++;
            } else {
                sw = Character.toString(charInput[i]);
            }
            switch (sw) {
                case "||":
                    while (!operationStack.empty() && !operationStack.peek().equals("(") && !operationStack.peek().equals(")")) {   //if the previous operations in the stack have higher priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    operationStack.push(String.valueOf(sw));
                    result.append(' ');
                    break;
                case "&&":
                    while (!operationStack.empty() && (operationStack.peek().equals("!") || operationStack.peek().equals("&&"))) {   //if the previous operations in the stack have higher priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    operationStack.push(String.valueOf(sw));
                    result.append(' ');
                    break;
                case "!":
                    while (!operationStack.empty() && operationStack.peek().equals("!")) {
                        result.append(' ').append(operationStack.pop());
                    }
                    operationStack.push(String.valueOf(sw));
                    break;
                case "(":
                    operationStack.push(String.valueOf(sw));
                    break;
                case " ":
                    break;
                case ")":
                    while (!operationStack.empty() && !operationStack.peek().equals("(")) {   // pop everything from stack to the result until we get to the '('
                        result.append(' ').append(operationStack.pop());
                    }
                    if (!operationStack.empty()) {    //remove the '('
                        operationStack.pop();
                    }
                    break;
                default:
                    result.append(sw);    // we have a letter
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
        StringTokenizer tokens = new StringTokenizer(input);
        Stack<Boolean> stack = new Stack<>();
        while (tokens.hasMoreTokens()) {
            String current = tokens.nextToken();
            if (Type.isBoolean(current)) {
                stack.push(Boolean.parseBoolean(current));
            } else {
                if(stack.empty()) {
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                }
                Boolean right = stack.pop();
                Boolean left;
                switch (current) {
                    case "!":
                        stack.push(!right);
                        break;
                    case "&":
                        left = stack.pop();
                        stack.push(Boolean.logicalAnd(left, right));
                        break;
                    case "|":
                        left = stack.pop();
                        stack.push(Boolean.logicalOr(left, right));
                        break;
                    case "&&":
                        left = stack.pop();
                        stack.push(left && right);
                        break;
                    case "||":
                        left = stack.pop();
                        stack.push(left || right);
                        break;
                    default:
                        throw new RuntimeException(INVALID_INPUT_MESSAGE);
                } //end of switch
            }  // end of if/else
        }     //end of while
        return stack.pop().toString();   //the result is in the stack(last element)
    }   //end of calculate RPN

    public static boolean executeBooleanOperation(String leftSide, String rightSide, String data) {
        if(data.equals("||")) {
            return leftSide.equalsIgnoreCase("TRUE") || rightSide.equalsIgnoreCase("TRUE");
        }
        return leftSide.equalsIgnoreCase("TRUE") && rightSide.equalsIgnoreCase("TRUE");
    }
}
