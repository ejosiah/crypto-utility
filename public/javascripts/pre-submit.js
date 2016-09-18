/**
 * save form data in session for use in the next request
 * this is required because we do not have form data available
 * to when we are streaming multipart files
 */
(function(app, $){
    $(app.document).ready(function(){
        $('form.pre-process').submit(function(e){
            e.preventDefault();
            var form = $(this);
            var action = form.prop("action");
            var query = {};
            form.find("input, select").not("input[type='file'], input[type='submit']").each(function(i, elm){
                var name = $(elm).prop('name');
               query[name] = $(elm).val()
            });
            action = action + Object.keys(query).reduce(function(acc, name){
                    return acc + name + "=" + query[name] + "&"
             }, "?").slice(0, -1);
            form.prop("action", action);
            this.submit();

        })
    });
}(window, jQuery));