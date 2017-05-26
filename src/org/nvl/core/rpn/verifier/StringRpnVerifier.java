/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.org.nvl.core.rpn.verifier;

import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.variable.Type;

import java.util.Stack;

import static src.org.nvl.MessageConstants.EMPTY_STACK_MESSAGE;
import static src.org.nvl.MessageConstants.IMPOSSIBLE_OPERATION_MESSAGE;
import static src.org.nvl.MessageConstants.INVALID_INPUT_MESSAGE;

/**
 * @author niki
 */
public class StringRpnVerifier extends AbstractRpnVerifier {

    @Override
    public String createRpn(String input) {  //creates the Reverse Polish Notation
        StringBuilder result = new StringBuilder();   //builder for the final result (RPN)
        boolean isInString = false;
        Stack<Character> operationStack = new Stack<>();  //sttack for the operation
        char[] charInput = input.toCharArray();  //char array for the input
        int i = 0;
        while (i < charInput.length) {   //iterrate through the input
            if(charInput[i] == '\'')
                isInString = !isInString;
            switch (charInput[i]) {
                case '+':
                case '-':
                    while (!operationStack.empty() && operationStack.peek() != '(' && operationStack.peek() != ')') {   //if the previous operations in the stack have higher priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    result.append(' ');
                    operationStack.push(charInput[i]);
                    break;
                case '*':
                case '/':
                    while(!operationStack.empty() && (operationStack.peek() == '*' || operationStack.peek() == '/')) {
                        result.append(' ').append(operationStack.pop());
                    }
                    result.append(' ');
                    operationStack.push(charInput[i]);
                    break;
                case '(':
                    operationStack.push(charInput[i]);
                    break;
                case ' ':
                    if(isInString)
                        result.append(charInput[i]);
                    break;
                case ')':
                    while (!operationStack.empty() && operationStack.peek() != '(') {   // pop everything from stack to the result until we get to the '('
                        result.append(' ').append(operationStack.pop());
                    }
                    if (!operationStack.empty()) {    //remove the '('
                        operationStack.pop();
                    }
                    break;
                case '!':
                case '^':
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                default:
                    result.append(charInput[i]);    // we have a char or '
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
    //calculate the reverse polish notation for strings
    public String calculateRpn(String input) {
        SplitString tokens = new SplitString(input);
        Stack<String> stack = new Stack<>();  //stack for the numbers
        while (!tokens.isEmpty()) {   //while we have more tokens
            String current = tokens.getCurrentElement();
            switch (current) {        //switch to see if current string is operation or string
                case "+":
                    plus(stack);
                    break;
                case "*":
                    multiply(stack);
                    break;
                case "-":
                    subtract(stack);
                    break;
                case "/":
                    divide(stack);
                    break;
                default:
                    stack.push(current);       //current is string
                    break;
            }   //end of switch
            tokens.nextPosition();
        }   //end of while
        if(stack.empty()) {
            throw new RuntimeException(EMPTY_STACK_MESSAGE);
        }
        return stack.pop();     //return result
    } //end of calculateRpnForStrings

    //divide if possible
    private void divide(Stack<String> stack) {
        if(stack.empty()) {
            throw new RuntimeException(EMPTY_STACK_MESSAGE);
        }
        String right = stack.pop();
        if(stack.empty() || !Type.isNumber(right) || right.equals("0")) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }
        String left = stack.pop();
        Integer numberOfOccurences = Integer.valueOf(right);

        //the length of the string without the quotation marks must be a multiple of 'numberOfOccurences'
        //the substring starts at index 1 (without quotation mark) and continues (left.length()-2)/numberOfOccurences) chars
        if(((left.length()- 2) % numberOfOccurences != 0) ||
                (countMatches(left, left.substring(1, 1 + (left.length() - 2)/numberOfOccurences)) != numberOfOccurences)) {
            throw new RuntimeException(IMPOSSIBLE_OPERATION_MESSAGE);
        }
        stack.push("'" + left.substring(1, 1 + (left.length() - 2)/numberOfOccurences) + "'");
    }

    //subtract two strings if possible
    private void subtract(Stack<String> stack) {
        if(stack.empty()) {
            throw new RuntimeException(EMPTY_STACK_MESSAGE);
        }
        String right = stack.pop();
        if(stack.empty()) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }
        String left = stack.pop();

        if(right.length() > 0 && !left.endsWith(right.substring(1, right.length()))) {
            throw new RuntimeException(IMPOSSIBLE_OPERATION_MESSAGE);
        }
        stack.push(left.substring(0, left.length() - (right.length() - 1)) + "'");
    }

    //concatenate the top 2 strings
    private void plus(Stack<String> stack) {
        String right = stack.pop();
        if(stack.empty()) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }
        String left = stack.pop();

        String leftWithoutQuoute = left.substring(0, left.length() - 1);
        String rightWithoutQuoute = right.substring(1, right.length());
        stack.push(leftWithoutQuoute.concat(rightWithoutQuoute));
    } //end of plus

    //concatenate string given amount of times
    private void multiply(Stack<String> stack) {
        boolean leftIsNumber = false, rightIsNumber = false;    //at least one will be a number
        Object left, right;            //values for the operation
        int count;      //how many times to concatenate
        String strToConcatenate;
        if (Type.isNumber(stack.peek())) {   //if right is number
            right = Integer.parseInt(stack.pop());
            rightIsNumber = true;
        } else {
            right = stack.pop();        //else its string
        }

        if(stack.empty()) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }
        if (Type.isNumber(stack.peek())) {
            left = Integer.parseInt(stack.pop());          //if left is number
            leftIsNumber = true;
        } else {
            left = stack.pop();         //else its string
        }

        if (leftIsNumber && rightIsNumber) {         //both are numbers
            stack.push(Integer.toString((Integer) left * (Integer) right));  // so we parse them to ints, multiply them and push it to stack
        } else {                                      //only one is number
            if (leftIsNumber) {    //left is number
                count = (int) left;   //concatenate left times
                strToConcatenate = (String) right;   //the right string
            } else {   //right is number
                count = (int) right;   //concatenate right times
                strToConcatenate = (String) left;       //the left string
            }
            StringBuilder sb = new StringBuilder();   //create the resulted string
            for (int i = 0; i < count; i++) {
                sb.append(strToConcatenate);
            }
            stack.push(sb.toString().replaceAll("\\'\\'", ""));   //push the resulted string
        }       //end of if
    }//end of multiply

    private int countMatches(String string, String substring) {
        int lastIndex = string.indexOf(substring);
        int count = 0;

        while(lastIndex != -1){
            lastIndex = string.indexOf(substring, lastIndex + 1);
            count ++;
        }

        return count;
    }
}
