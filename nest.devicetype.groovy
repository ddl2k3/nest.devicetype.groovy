/**
 *  Nest Direct
 *
 *  Author: dianoga7@3dgo.net
 *  Date: 2013-07-18
 */
 

def setup() {
    data.username = ''
    data.password = ''
    data.serial = ''    
}
 
 // for the UI
metadata {
    simulator {
        // TODO: define status and reply messages here
    }

    tiles {
        valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: true) {
            state("temperature", label: '${currentValue}°', unit:"F", backgroundColors: [
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
        }
        standardTile("thermostatMode", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
            state "heat", label:'${name}', action:"thermostat.off", icon: "st.Weather.weather14"
            state "off", label:'${name}', action:"thermostat.cool", icon: "st.Outdoor.outdoor19"
            state "cool", label:'${name}', action:"thermostat.heat", icon: "st.Weather.weather7"
        }
        standardTile("thermostatFanMode", "device.thermostatFanMode", inactiveLabel: false, decoration: "flat") {
            state "auto", label:'${name}', action:"thermostat.fanOn", icon: "st.Appliances.appliances11"
            state "on", label:'${name}', action:"thermostat.fanCirculate", icon: "st.Appliances.appliances11"
            state "circulate", label:'${name}', action:"thermostat.fanAuto", icon: "st.Appliances.appliances11"
        }
        controlTile("coolSliderControl", "device.coolingSetpoint", "slider", height: 1, width: 3, inactiveLabel: false) {
            state "setCoolingSetpoint", label:'Set temperarure to', action:"thermostat.setCoolingSetpoint", 
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]               
        }
        valueTile("coolingSetpoint", "device.coolingSetpoint", inactiveLabel: false, decoration: "flat") {
            state "default", label:'${currentValue}°', unit:"F", backgroundColor:"#ffffff", icon:"st.appliances.appliances8"
        }
        valueTile("humidity", "device.humidity", inactiveLabel: false, decoration: "flat") {
            state "default", label:'${currentValue}%', unit:"Humidity"
        }
        standardTile("away", "device.away", inactiveLabel: false, decoration: "flat") {
            state "present", label:'${name}', action:"custom.away", icon: "st.Home.home2"
            state "away", label:'${name}', action:"custom.present", icon: "st.Transportation.transportation5"
        }
        standardTile("refresh", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
        main "temperature"
        details(["temperature", "thermostatMode", "thermostatFanMode", "coolSliderControl", "coolingSetpoint", "humidity", "away", "refresh"])
    }
}

// parse events into attributes
def parse(String description) {
    
}

// handle commands
def setHeatingSetpoint(temp) {
    setTargetTemp(temp);
}

def setCoolingSetpoint(temp) {
    setTargetTemp(temp);
}

def setTargetTemp(temp) {
    api('temperature', ['target_change_pending': true, 'target_temperature': fToC(temp)]) {
        sendEvent(name: 'coolingSetpoint', value: temp)
        sendEvent(name: 'heatingSetpoint', value: temp)
    }
}

def off() {
    setThermostatMode('off')
}

def heat() {
    setThermostatMode('heat')
}

def emergencyHeat() {
    setThermostatMode('heat')
}

def cool() {
    setThermostatMode('cool')
}

def setThermostatMode(mode) {
    mode = mode == 'emergency heat'? 'heat' : mode
    
    api('thermostat_mode', ['target_change_pending': true, 'target_temperature_type': mode]) {
        sendEvent(name: 'thermostatMode', value: mode)
    }
}

def fanOn() {
    setThermostatFanMode('on')
}

def fanAuto() {
    setThermostatFanMode('auto')
}

def fanCirculate() {
    setThermostatFanMode('circulate')
}

def setThermostatFanMode(mode) {    
    def modes = [
        on: ['fan_mode': 'on'],
        auto: ['fan_mode': 'auto'],
        circulate: ['fan_mode': 'duty-cycle', 'fan_duty_cycle': 900]
    ]
    
    api('fan_mode', modes.getAt(mode)) {
        sendEvent(name: 'thermostatFanMode', value: mode)
    }
}

def away() {
    setAway('away')
}

def present() {
    setAway('present')
}

def setAway(status) {
    api('away', ['away': status == 'away', 'away_timestamp': new Date().getTime(), 'away_setter': 0]) {
        sendEvent(name: 'away', value: status)
    }
}

def auto() {
    log.debug "Executing 'auto'"
}

def poll() {
    log.debug "Executing 'poll'"
    api('status', []) {
        data.device = it.data.device.getAt(data.serial)
        data.shared = it.data.shared.getAt(data.serial)
        data.structureId = it.data.link.getAt(data.serial).structure.tokenize('.')[1]
        data.structure = it.data.structure.getAt(data.structureId)
                
        data.device.fan_mode = data.device.fan_mode == 'duty-cycle'? 'circulate' : data.device.fan_mode
        data.structure.away = data.structure.away? 'away' : 'present'
                
        sendEvent(name: 'humidity', value: data.device.current_humidity)
        sendEvent(name: 'temperature', value: cToF(data.shared.current_temperature) as Integer, state: data.device.target_temperature_type)
        sendEvent(name: 'thermostatFanMode', value: data.device.fan_mode)
        sendEvent(name: 'thermostatMode', value: data.shared.target_temperature_type)
        sendEvent(name: 'coolingSetpoint', value: cToF(data.shared.target_temperature) as Integer)
        sendEvent(name: 'heatingSetpoint', value: cToF(data.shared.target_temperature) as Integer)
        sendEvent(name: 'away', value: data.structure.away)
    }
}

def api(method, args = [], success = {}) {
    if(!isLoggedIn()) {
        log.debug "Need to login"
        login(method, args, success)
        return
    }

    def methods = [
        'status': [uri: "/v2/mobile/${data.auth.user}", type: 'get'],
        'fan_mode': [uri: "/v2/put/device.${data.serial}", type: 'post'],
        'thermostat_mode': [uri: "/v2/put/shared.${data.serial}", type: 'post'],
        'temperature': [uri: "/v2/put/shared.${data.serial}", type: 'post'],
        'away': [uri: "/v2/put/structure.${data.structureId}", type: 'post'],
        'weather': [uri: "https://home.nest.com/api/0.1/weather/forecast/52317", type: 'get']
    ]
    
    def request = methods.getAt(method)
    
    log.debug "Logged in"
    doRequest(request.uri, args, request.type, success)
}

// Need to be logged in before this is called. So don't call this. Call api.
def doRequest(uri, args, type, success) {
    log.debug "Calling $type : $uri : $args"
    
    if(uri.charAt(0) == '/') {
        uri = "${data.auth.urls.transport_url}${uri}"
    }
    
    def params = [
        uri: uri,
        headers: [
            'X-nl-protocol-version': 1,
            'X-nl-user-id': data.auth.userid,
            'Authorization': "Basic ${data.auth.access_token}"
        ],
        body: args
    ]
    
    if(type == 'post') {
        httpPostJson(params, success)
    } else if (type == 'get') {
        httpGet(params, success)
    }
}

def login(method = null, args = [], success = {}) {
    setup()
    
    def params = [
        uri: 'https://home.nest.com/user/login',
        body: [username: data.username, password: data.password]
    ]        
    
    httpPost(params) {response -> 
        data.auth = response.data
        data.auth.expires_in = Date.parse('EEE, dd-MMM-yyyy HH:mm:ss z', response.data.expires_in).getTime()
        log.debug data.auth
        
        api(method, args, success)
    }
}

def isLoggedIn() {
    if(!data.auth) {
        log.debug "No data.auth"
        return false
    }
    
    def now = new Date().getTime();
    return data.auth.expires_in > now
}

def cToF(temp) {
    return temp * 1.8 + 32
}

def fToC(temp) {
    return (temp - 32) / 1.8
}