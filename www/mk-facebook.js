var exec = require('cordova/exec');

exports.mkFacebook = {
    shareScreen: function(success, error) {
        exec(success, error, "MkFacebook", "shareScreen");
    }
};
