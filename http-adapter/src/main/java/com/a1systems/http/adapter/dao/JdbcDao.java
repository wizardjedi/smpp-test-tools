package com.a1systems.http.adapter.dao;

import com.a1systems.http.adapter.message.Message;
import com.a1systems.http.adapter.message.MessagePart;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcDao implements Dao {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Override
    public MessagePart getMessagePartById(Long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MessagePart getMessagePartBySMSCId(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Message getMessageById() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Message saveMessage(Message message) {
        String sql = "INSERT INTO `messages` (`id`,`abonent`,`sender`,`text`) VALUES (?,?,?,?);";

        //jdbcTemplate.execute("BEGIN;");

        jdbcTemplate
            .update(
                sql,
                new Object[] {
                    message.getId(),
                    message.getDestination(),
                    message.getSource(),
                    message.getMessage()
                }
            );

        final Message msg = message;

        String partSql = "INSERT INTO `message_parts` (`id`,`message_id`,`text`) VALUES (?,?,?);";

        final List<MessagePart> partList = Arrays.asList(message.getParts().toArray(new MessagePart[]{}));

        jdbcTemplate
                .batchUpdate(
                    partSql,
                    new BatchPreparedStatementSetter() {

                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            MessagePart part = partList.get(i);

                            ps.setLong(1, part.getId());
                            ps.setLong(2, msg.getId());
                            ps.setBytes(3, part.getShortMessage());
                        }

                        @Override
                        public int getBatchSize() {
                            return partList.size();
                        }
                    }
                );

        //jdbcTemplate.execute("COMMIT;");

        return message;
    }

    @Override
    public MessagePart saveMessagePart(MessagePart messagePart) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MessagePart saveSMSCId(MessagePart messagePart) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
