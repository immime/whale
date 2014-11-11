package org.octopus.iot.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.View;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Fail;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.Param;
import org.octopus.iot.IotKeys;
import org.octopus.iot.bean.IotSensor;
import org.octopus.iot.bean.IotUser;
import org.octopus.iot.service.UserService;

@IocBean
@At("/iot2/mqtt/auth")
public class MqttAuthModule {

	@Inject Dao dao;
	
	@Inject UserService userService; // 主体User,不是IotUser哦
	
	static View HTTP_403 = new View() {
		
		public void render(HttpServletRequest req, HttpServletResponse resp, Object obj) throws Throwable {
			resp.setStatus(403);
		}
	};
	
	private static final Log log = Logs.get();
	
	@At("/user")
	@Ok("void")
	public View user(@Param("username")String username, @Param("password")String password) {
		log.infof("u=%s p=%s", username, password);;
		if (Strings.isBlank(username))
			return HTTP_403;
		if (dao.count(IotUser.class, Cnd.where("name", "=", username).and("apikey", "=", password)) == 1) {
			return null;
		}
		return HTTP_403;
	}
	
	@At("/super")
	@Ok("void")
	public View isSuperUser(@Param("username")String username) {
		if ("admin".equals(username))
			return null;
		return HTTP_403;
	}
	
	@At("/acl")
	@Ok("void")
	@Fail("http:403")
	public View acl(@Param("username")String username, @Param("topic")String topic, @Param("acc")String acc) {
		if (!"1".equals(acc))
			return HTTP_403; // TODO 支持mqtt发布, 即通过mqtt更新传感器的值
		if (Strings.isBlank(topic) || !topic.matches("^iot2/sensor/[0-9]+$"))
			return HTTP_403;
		long sensor_id = Long.parseLong(topic.substring("iot2/sensor/".length()));
		IotSensor sensor = dao.fetch(IotSensor.class, Cnd.where(IotKeys.UID, "=", userService.userId(username)).and("id", "=", sensor_id));
		if (sensor == null)
			return HTTP_403;
		return null;
	}
}
