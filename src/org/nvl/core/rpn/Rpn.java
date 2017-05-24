package src.org.nvl.core.rpn;

import src.org.nvl.core.input.type.SideType;
import src.org.nvl.core.rpn.verifier.ArrayRpnVerifier;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;
import src.org.nvl.core.rpn.verifier.StringRpnVerifier;

/**
 * Created by Vicky on 23.5.2017 г..
 */
public class Rpn {
    public static AbstractRpnVerifier makeRpn(SideType type) {
        AbstractRpnVerifier rpnVerifier;
        if (type == SideType.ARRAY) {
            rpnVerifier = new ArrayRpnVerifier();
        } else if (type == SideType.STRING) {
            rpnVerifier = new StringRpnVerifier();
        } else if (type == SideType.NUMBER) {
            rpnVerifier = new NumberRpnVerifier();
        } else {
            rpnVerifier = new BooleanRpnVerifier();
        }
        return rpnVerifier;
    }
}
