package propnet.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import propnet.vanilla.PropNet;
import propnet.vanilla.core.Node;
import propnet.vanilla.core.NodeFns;
import rekkura.logic.model.Dob;
import rekkura.logic.structure.Pool;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: david
 * Date: 7/15/13
 * Time: 10:45 AM
 * Description:
 *      Basically for copying propnet
 */
public class PropNetUtil {

    // Copies propnet into new context
    public static PropNet copyPropNetVanilla(PropNet toCopy, Pool newPool){
        List<Node> copiedTnet = Lists.newArrayList();
        Map<Node, Node> copied = Maps.newHashMap();
        Map<Dob, Node> copiedProps = Maps.newHashMap();

        // copy tnet
        for (Node node : toCopy.tnet) {
            Node copyNode;
            Set<Node> copyInputs = Sets.newHashSet();
            for (Node input : node.inputs) {
                Preconditions.checkArgument(copied.containsKey(input));
                copyInputs.add(copied.get(input));
            }
            if (node.fn == NodeFns.AND) {
                copyNode = Node.NodeFactory.makeAnd(copyInputs);
            } else if (node.fn == NodeFns.OR) {
                copyNode = Node.NodeFactory.makeOr(copyInputs);
            } else if (node.fn == NodeFns.NOT) {
                copyNode = Node.NodeFactory.makeNot(copyInputs);
            } else {
                break;
            }
            copiedTnet.add(copyNode);
            copied.put(node, copyNode);
        }

        // copy props map
        for (Dob prop : toCopy.props.keySet()) {
            Dob copiedProp = newPool.dobs.submerge(prop);
            copiedProps.put(copiedProp, copied.get(toCopy.props.get(prop)));
        }

        return new PropNet(copiedProps, copiedTnet);
    }
}
