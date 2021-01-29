package com.lhy.mucAllMembers;

import com.lhy.mucAllMembers.dao.ChatRoomDao;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * https://blog.csdn.net/lhy24680/article/details/113353293
 * 自定义IQ获取所有群成员信息
 */
public class MucAllMembersPlugin implements Plugin {

    private IQRouter iqRouter;
    private  final Logger Log = LoggerFactory.getLogger(MucAllMembersPlugin.class);

    /**
     * 插件初始化，启动openfire时会走这一步
     * @param manager the plugin manager.
     * @param pluginDirectory the directory where the plugin is located.
     */
    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        // TODO Auto-generated method stub
        Log.info("------------------->MatchUserPlugin initialize!");
        try {
            iqRouter = XMPPServer.getInstance().getIQRouter();
            iqRouter.addHandler(new MyIQHandler("MyIQHandler"));
        } catch (Exception e) {
            Log.info("------------------->Bind Exception:"+e.toString());
        }
    }

    @Override
    public void destroyPlugin() {
        // TODO Auto-generated method stub
        Log.info("------------------->MatchUserPlugin destroy!");
    }

    /**
     * IQ数据包处理器
     * @author Administrator
     *
     */
    private class MyIQHandler extends IQHandler {

        /**
         * 约定的请求名，相当于接口名
         */
        private static final String GET_MUC_ALL_MEMBERS="get_muc_all_members";

        /**
         * 数据包的namespace
         */
        private static final String NAMESPACE_INFO="match:iq:info";

        private IQHandlerInfo info;

        @Override
        public void initialize(XMPPServer server) {
            // TODO Auto-generated method stub
            super.initialize(server);

        }
        public MyIQHandler(String moduleName) {
            super(moduleName);
            // TODO Auto-generated constructor stub
            info = new IQHandlerInfo("info", NAMESPACE_INFO);
        }

        @Override
        public IQHandlerInfo getInfo() {
            // TODO Auto-generated method stub
            return info;
        }

        @Override
        public IQ handleIQ(IQ packet) throws UnauthorizedException {
            // TODO Auto-generated method stub
            /**
             * replyIQ初始结构
             * <iq type="result"
             * id="TeUQc-12"
             * from="localhost/MatchUserPlugin"
             * to="zyw8@localhost/Smack"/>
             *
             */
            String childName=packet.getChildElement().getName();

            if(childName==null) {
                return packet;
            }
            if(!childName.equals("info")) {
                return packet;
            }

            IQ replyIQ = IQ.createResultIQ(packet);
            ClientSession clientSession = sessionManager.getSession(packet.getFrom());

            if(clientSession==null) {
                log("Error user info.Session not found"
                        +sessionManager.getPreAuthenticatedKeys()
                        +" from "+packet.getFrom());
                replyIQ.setChildElement(packet.getChildElement().createCopy());
                replyIQ.setError(PacketError.Condition.forbidden);
                return replyIQ;
            }

            org.dom4j.Element child = packet.getChildElement().createCopy();
            // 客户端请求时携带的请求名
            String type = child.attributeValue("type");
            String ns = child.getNamespaceURI();
            // 客户端请求时携带的数据
            String roomName = child.attributeValue("data");
            if(!ns.equals(NAMESPACE_INFO)) {
                log("This namespace is valid !"
                        +sessionManager.getPreAuthenticatedKeys()
                        +" from "+packet.getFrom());
                replyIQ.setChildElement(packet.getChildElement().createCopy());
                replyIQ.setError(PacketError.Condition.bad_request);
                return replyIQ;
            }

            // 从数据库获取群内所有成员，按权限排序
            List<Map<String, String>> data = ChatRoomDao.getMucAllMembers(roomName);
            Map<String, String> map = null;

            Element infoElement = DocumentHelper.createElement("info");
            infoElement.addNamespace("", NAMESPACE_INFO);
            switch (type) {
                case GET_MUC_ALL_MEMBERS:
                    try {
                        //
                        for (int i = 0; i < data.size(); i++) {
                            map = data.get(i);
                            // 将房间成员jid和affiliation信息放入子节点
                            Element roomMember = infoElement.addElement("roomMember");
                            roomMember.addAttribute("jid", map.get("jid"));
                            roomMember.addAttribute("affiliation", map.get("affiliation"));
                        }
                        infoElement.addAttribute("type", GET_MUC_ALL_MEMBERS);
                        infoElement.addAttribute("data", roomName);
                        replyIQ.setChildElement(infoElement);
                    } catch (Exception e) {
                        // TODO: handle exception
                        log("------------------->ERROR:"+e.toString());
                    }
                    break;
                default:
                    return packet;
            }
            return replyIQ;
        }

        public void log(String text) {
            Log.info(text);
        }

    }

}
