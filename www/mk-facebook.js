var exec = require('cordova/exec');

exports.shareScreen = function shareScreen (s, f) {    
  exec(s, f, 'MkFacebook', 'shareScreen', []);
};
