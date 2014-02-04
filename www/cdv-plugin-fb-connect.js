CDV = (typeof CDV == 'undefined' ? {} : CDV);
var cordova = window.cordova || window.Cordova;
var fbClassName = 'ConnectPlugin';
CDV.FBClass = {
    init: function (apiKey, fail) {
        // create the fb-root element if it doesn't exist
        if (!document.getElementById('fb-root')) {
            var elem = document.createElement('div');
            elem.id = 'fb-root';
            document.body.appendChild(elem);
        }
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onload = function () {
            console.log("Endpoint saved " + this.responseText);
        }
        xmlhttp.open("POST", "https://www.facebook.com/impression.php", true);
        xmlhttp.send('plugin=featured_resources&payload={"resource": "adobe_phonegap", "appid": "' + apiKey + '", "version": "3.0.0" }');

        cordova.exec(function () {
            var authResponse = JSON.parse(localStorage.getItem('cdv_fb_session') || '{"expiresIn":0}');
            if (authResponse && authResponse.expirationTime) {
                var nowTime = (new Date()).getTime();
                if (authResponse.expirationTime > nowTime) {
                    // Update expires in information
                    updatedExpiresIn = Math.floor((authResponse.expirationTime - nowTime) / 1000);
                    authResponse.expiresIn = updatedExpiresIn;

                    localStorage.setItem('cdv_fb_session', JSON.stringify(authResponse));
                    FB.Auth.setAuthResponse(authResponse, 'connected');
                }
            }
            console.log('Cordova Facebook Connect plugin initialized successfully.');
        }, (fail ? fail : null), fbClassName, 'init', [apiKey]);
    },
    login: function (params, cb, fail) {
        params = params || {
            scope: ''
        };
        cordova.exec(function (e) { // login
            if (e.authResponse && e.authResponse.expiresIn) {
                var expirationTime = e.authResponse.expiresIn === 0 ? 0 : (new Date()).getTime() + e.authResponse.expiresIn * 1000;
                e.authResponse.expirationTime = expirationTime;
            }
            localStorage.setItem('cdv_fb_session', JSON.stringify(e.authResponse));
            if(typeof FB !== 'undefined' && typeof FB.Auth !== 'undefined' && FB.Auth.setAuthResponse !== 'undefined')
                FB.Auth.setAuthResponse(e.authResponse, 'connected');
            if (cb) cb(e);
        }, (fail ? fail : null), fbClassName, 'login', params.scope.split(','));
    },
    logout: function (cb, fail) {
        cordova.exec(function (e) {
            localStorage.removeItem('cdv_fb_session');
            if(typeof FB !== 'undefined' && typeof FB.Auth !== 'undefined' && FB.Auth.setAuthResponse !== 'undefined')
                FB.Auth.setAuthResponse(null, 'notConnected');
            if (cb) cb(e);
        }, (fail ? fail : null), fbClassName, 'logout', []);
    },
    getLoginStatus: function (cb, fail) {
        cordova.exec(function (e) {
            if (cb) cb(e);
        }, (fail ? fail : null), fbClassName, 'getLoginStatus', []);
    },
    dialog: function (params, cb, fail) {
        
        var link = params.link;
        
        if(typeof link !=='undefined'){
           
            if(link.indexOf(encodeURIComponent('file:///'))==0){
               
                params.link = link.replace(encodeURIComponent('file:///'),'http://staging.rundavoo.com/');
                
            }
        }
        
        cordova.exec(function (e) { // login
            if (cb) cb(e);
        }, (fail ? fail : null), fbClassName, 'showDialog', [params]);
    }
};
module.exports = CDV.FBClass;