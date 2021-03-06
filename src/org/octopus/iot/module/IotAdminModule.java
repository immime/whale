package org.octopus.iot.module;

import java.util.Collections;
import java.util.List;

import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.adaptor.JsonAdaptor;
import org.nutz.mvc.annotation.AdaptBy;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Attr;
import org.nutz.mvc.annotation.By;
import org.nutz.mvc.annotation.DELETE;
import org.nutz.mvc.annotation.Fail;
import org.nutz.mvc.annotation.Filters;
import org.nutz.mvc.annotation.GET;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.POST;
import org.nutz.mvc.annotation.PUT;
import org.nutz.mvc.annotation.Param;
import org.octopus.iot.IotKeys;
import org.octopus.iot.Iots;
import org.octopus.iot.bean.IotDevice;
import org.octopus.iot.bean.IotSensor;
import org.octopus.iot.bean.IotUser;
import org.octopus.iot.bean.IotUserLevel;
import org.octopus.iot.mvc.ApiKeyFilter;
import org.octopus.iot.service.IotService;

@Filters({@By(type=ApiKeyFilter.class, args="ioc:apiKeyFilter")})
@Fail("http:500")
@Ok("json:{autoUnicode:true,quoteName:true,compact:true}")
@IocBean
@At("/iot2")
public class IotAdminModule {
	
	@Inject
	Dao dao;
	
	@Inject IotService iotService;
	
	@At("/ukey")
	@GET
	@Filters()
	public Object readApikey(@Attr(IotKeys.UID)long userId) {
		if (userId == 0)
			return Collections.EMPTY_MAP;
		IotUser usr = dao.fetch(IotUser.class, userId);
		if (usr == null) {
			usr = new IotUser();
			iotService.makeApiKey(usr);
			usr.setUserLevel(IotUserLevel.FREE);
			dao.insert(usr);
		}
		return new NutMap().addv("apikey", usr.getApikey());
	}
	
	@At("/ukey/reset")
	@GET
	@Filters()
	public Object resetApikey(@Attr(IotKeys.UID)long userId) {
		if (userId == 0)
			return Collections.EMPTY_MAP;
		IotUser usr = dao.fetch(IotUser.class, userId);
		if (usr == null) {
			usr = new IotUser();
			iotService.makeApiKey(usr);
			dao.insert(usr);
		} else {
			iotService.makeApiKey(usr);
			dao.update(usr);
		}
		return new NutMap().addv("apikey", usr.getApikey());
	}
	
	@At({"/devices"})
	@GET
	public List<IotDevice> listDev(@Attr(IotKeys.UID)long userId) {
		return dao.query(IotDevice.class, Cnd.where(IotKeys.UID, "=", userId));
	}
	
	@At({"/device/?/sensors"})
	@GET
	@Filters()
	// TODO 区分private和public设备
	public List<IotSensor> listSensors(long deviceId, @Attr(IotKeys.UID)long userId) throws IllegalAccessException {
		return dao.query(IotSensor.class, Cnd.where("deviceId", "=", deviceId));
	}
	
	@At({"/devices"})
	@POST
	@AdaptBy(type=JsonAdaptor.class)
	public IotDevice createDev(@Param("..")IotDevice dev, @Attr(IotKeys.UID)long userId) {
		if (dev == null) {
			return null;
		}
		int devCount = dao.count(IotDevice.class, Cnd.where(IotKeys.UID, "=", userId));
		if (devCount > Iots.Limit_Dev_Per_User) { // TODO 可扩展,可配置
			return null;
		}
		dev.setId(userId);
		dev.setSensors(null);
		return dao.insert(dev);
	}
	
	@At({"/device/?"})
	@GET
	public IotDevice getDev(long device_id, @Param("..")IotDevice dev, @Attr(IotKeys.UID)long userId) {
		return dao.fetch(IotDevice.class, Cnd.where("id", "=", device_id).and(IotKeys.UID, "=", userId));
	}
	
	@At({"/device/?"})
	@PUT
	@AdaptBy(type=JsonAdaptor.class)
	public IotDevice updateDev(long device_id, @Param("..")IotDevice dev, @Attr(IotKeys.UID)long userId) {
		if (dev == null || 0 == dao.count(IotDevice.class, Cnd.where("id", "=", device_id).and(IotKeys.UID, "=", userId))) {
			return null;
		}
		dev.setId(device_id);
		dev.setUserId(userId);
		dao.update(dev);
		return dev;
	}
	
	@At({"/device/?"})
	@DELETE
	@Ok("void")
	public void deleteDev(long device_id, @Attr(IotKeys.UID)long userId) {
		dao.clear(IotDevice.class, Cnd.where("id", "=", device_id).and(IotKeys.UID, "=", userId));
		dao.clear(IotSensor.class, Cnd.where("deviceId", "=", device_id).and(IotKeys.UID, "=", userId));
	}
	
	@At({"/device/?/sensor/?"})
	@GET
	@Filters()
	public IotSensor getSensor(long device_id, long sensor_id) {
		return dao.fetch(IotSensor.class, Cnd.where("deviceId", "=", device_id).and("id", "=", sensor_id));
	}
	
	@At({"/device/?/sensors"})
	@POST
	@AdaptBy(type=JsonAdaptor.class)
	public IotSensor createSensor(long device_id, @Param("..")IotSensor sensor, @Attr(IotKeys.UID)long userId) {
		if (sensor == null)
			return null;
		IotDevice dev = dao.fetch(IotDevice.class, Cnd.where("deviceId", "=", device_id).and(IotKeys.UID, "=", userId));
		if (dev == null)
			return null;
		int sensorCount = dao.count(IotSensor.class, Cnd.where("deviceId", "=", device_id).and(IotKeys.UID, "=", userId));
		if (sensorCount > Iots.Limit_Sensor_Per_Dev) {
			return null;
		}
		sensor.setDeviceId(device_id);
		sensor.setUserId(userId);
		dao.insert(sensor);
		return sensor;
	}
	
	@At({"/device/?/sensor/?"})
	@PUT
	@AdaptBy(type=JsonAdaptor.class)
	public IotSensor updateSensor(long device_id, @Param("..")IotSensor sensor, @Attr(IotKeys.UID)long userId) {
		if (sensor == null)
			return null;
		IotDevice dev = dao.fetch(IotDevice.class, Cnd.where("deviceId", "=", device_id).and(IotKeys.UID, "=", userId));
		if (dev == null)
			return null;
		sensor.setDeviceId(device_id);
		sensor.setUserId(userId);
		dao.update(sensor);
		return sensor;
	}
	
	@At({"/device/?/sensor/?"})
	@DELETE
	@Ok("http:200")
	public void deleteSensor(long device_id, @Param("sensor_id")long sensor_id, @Attr(IotKeys.UID)long userId) {
		dao.clear(IotSensor.class, Cnd.where("deviceId", "=", device_id).and(IotKeys.UID, "=", userId).and("id", "=", sensor_id));
	}
	
}
