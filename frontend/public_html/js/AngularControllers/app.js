/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 (function(){

 	var prodAndes= angular.module('ProdAndes',[]);

 	prodAndes.directive('toolbar', function(){
 		return{
 			restrict:'E',
 			templateUrl: 'partials/toolbar.html',
 			controller:function(){
 				this.tab=0;
                this.selectTab=function(setTab){
                    this.tab=setTab;
                };
                this.isSelected=function(tabParam){
                    return this.tab===tabParam;
                };
            },
            controllerAs:'toolbar'
        };
    });

 	prodAndes.directive('navegacion', function(){
 		return{
 			restrict:'E',
 			templateUrl: 'partials/navegacion.html',
 			controller:function(){
 				
 			},
 			controllerAs:'navegacion'
 		};
 	});


 	prodAndes.directive('registrarPedidoForm', function(){
        return{
            restrict:'E',
            templateUrl:'partials/registrar-pedido-form.html',
            controller: ['$http',function($http){
                var self=this;
                self.pedido={};
                this.addPedido=function(pedidoParam){

                	self.pedido = pedidoParam,
                	console.log('Form pedido '+JSON.stringify(self.pedido));
                    $http.post('http://localhost:8080/backend/ServiciosMock/registrarPedido'
                    	, self.pedido).success(function(data){
                         alert("Respuesta "+data.Respuesta);
                         self.pedido={};
                     });
                    };
                }],
                controllerAs:'registrarPedidoCtrl'
            };
        });

    prodAndes.directive('registrarEntregaPedidoForm', function(){
        return{
            restrict:'E',
            templateUrl:'partials/registrar-entrega-pedido-form.html',
            controller: ['$http',function($http){
                var self=this;
                self.pedido={};
                this.addPedido=function(pedidoParam){

                	self.pedido = pedidoParam,
                	console.log('Form pedido '+JSON.stringify(self.pedido));
                    $http.post('http://localhost:8080/backend/ServiciosMock/registrarEntregaPedidoProductosCliente'
                    	, self.pedido).success(function(data){
                         alert("Respuesta se ha registrado la entrega del pedido");
                         self.pedido={};
                     });
                    };
                }],
                controllerAs:'registrarEntregaPedidoCtrl'
            };
        });

    prodAndes.directive('toolbarConsultaProducto', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/toolbar-consulta-producto.html',
            controller:function(){
                this.tab=0;
                this.selectTab=function(setTab){
                    this.tab=setTab;
                };
                this.isSelected=function(tabParam){
                    return this.tab===tabParam;
                };
            },
            controllerAs:'toolbarConsultaProductoCtrl'
        };
    });

    prodAndes.directive('toolbarConsultaSuministros', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/toolbar-consulta-suministros.html',
            controller:function(){
                this.tab=0;
                this.selectTab=function(setTab){
                    this.tab=setTab;
                };
                this.isSelected=function(tabParam){
                    return this.tab===tabParam;
                };
            },
            controllerAs:'toolbarConsultaSuministrosCtrl'
        };
    });

    prodAndes.directive('consultarProductosForm', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/consultar-productos-form.html',
            controller: ['$http',function($http){
                var self = this;


                self.consulta = {};
                self.productos = [];

                this.isFull=function(){
                    return self.productos.length>0;
                };


                this.enviarConsulta=function(consultaParam,criterio){

                    self.productos = [];

                    console.log("Criterio "+criterio)
                    self.consulta = consultaParam,
                    self.consulta.Criterio = criterio;
                    console.log('Form consulta '+JSON.stringify(self.consulta));
                    $http.post('http://localhost:8080/backend/ServiciosMock/consultarProductos' , self.consulta).success(function(data){

                        console.log("Consultar productos "+JSON.stringify(data));
                        self.productos=data;
                        console.log("Consultar productos 2"+JSON.stringify(self.productos));
                        self.consulta={};
                    });


                };
            }],
            controllerAs:'consultarProductosCtrl'
        };
    });

    prodAndes.directive('listaProductosConsulta', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/lista-productos-consulta.html',
            controller:function(){

            },
            controllerAs:'listaProductosConsulta'
        };
    });


    prodAndes.directive('consultarMateriasForm', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/consultar-materias-form.html',
            controller: ['$http',function($http){
                var self = this;


                self.consulta = {};
                self.materias = [];

                this.isFull=function(){
                    return self.materias.length>0;
                };


                this.enviarConsulta=function(consultaParam,criterio){

                    self.materias = [];

                    console.log("Criterio "+criterio)
                    self.consulta = consultaParam,
                    self.consulta.Criterio = criterio;
                    console.log('Form consulta '+JSON.stringify(self.consulta));
                    $http.post('http://localhost:8080/backend/ServiciosMock/consultarMateriasPrimas', self.consulta).success(function(data){

                        console.log("Consultar materias "+JSON.stringify(data));
                        self.materias=data;
                        console.log("Consultar materias 2"+JSON.stringify(self.materias));
                        self.consulta={};
                    });


                };
            }],
            controllerAs:'consultarMateriasCtrl'
        };
    });

    prodAndes.directive('listaMateriasConsulta', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/lista-materias-consulta.html',
            controller:function(){

            },
            controllerAs:'listaMateriasConsulta'
        };
    });

    prodAndes.directive('consultarComponentesForm', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/consultar-componentes-form.html',
            controller: ['$http',function($http){
                var self = this;


                self.consulta = {};
                self.componentes = [];

                this.isFull=function(){
                    return self.componentes.length>0;
                };


                this.enviarConsulta=function(consultaParam,criterio){

                    self.componentes = [];

                    console.log("Criterio "+criterio)
                    self.consulta = consultaParam,
                    self.consulta.Criterio = criterio;
                    console.log('Form consulta '+JSON.stringify(self.consulta));
                    $http.post('http://localhost:8080/backend/ServiciosMock/consultarComponentes', self.consulta).success(function(data){

                        console.log("Consultar Componentes "+JSON.stringify(data));
                        self.componentes=data;
                        console.log("Consultar Componentes 2"+JSON.stringify(self.componentes));
                        self.consulta={};
                    });


                };
            }],
            controllerAs:'consultarComponentesCtrl'
        };
    });

    prodAndes.directive('listaComponentesConsulta', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/lista-componentes-consulta.html',
            controller:function(){

            },
            controllerAs:'listaComponentesConsulta'
        };
    });
    prodAndes.directive('registrarProveedorForm', function(){
        return{
            restrict:'E',
            templateUrl:'partials/registrar-proveedor-form.html',
            controller: ['$http',function($http){
                var self=this;
                self.proveedor={};
                this.addProveedor=function(proveedorParam){

                	self.proveedor = proveedorParam,
                	console.log('Que es esto '+JSON.stringify(proveedorParam));console.log('Form pedido '+JSON.stringify(self.proveedor));
                    $http.post('http://localhost:8080/backend/ServiciosMock/registrarProveedor'
                    	, self.proveedor).success(function(data){
                    	alert("Respuesta "+data.Respuesta);
                        self.proveedor={};
                    });
                };
            }],
            controllerAs:'registrarProveedorCtrl'
        };
    });
    
    prodAndes.directive('registrarLlegadaMaterialForm', function(){
        return{
            restrict:'E',
            templateUrl:'partials/registrar-llegada-material-form.html',
            controller: ['$http',function($http){
                var self=this;
                self.llegada={};
                this.addLlegadaMaterial=function(llegadaParam){

                	self.llegada = llegadaParam,
                	console.log('Que es esto '+JSON.stringify(llegadaParam));console.log('Form pedido '+JSON.stringify(self.llegada));
                    $http.post('http://localhost:8080/backend/ServiciosMock/llegadaMaterial'
                    	, self.llegada).success(function(data){
                    	alert("Respuesta "+data.Respuesta);
                        self.llegada={};
                    });
                };
            }],
            controllerAs:'registrarLlegadaMaterialCtrl'
        };
    });
    
    prodAndes.directive('registrarLlegadaComponenteForm', function(){
        return{
            restrict:'E',
            templateUrl:'partials/registrar-llegada-componente-form.html',
            controller: ['$http',function($http){
                var self=this;
                self.llegada={};
                this.addLlegadaComponente=function(llegadaParam){

                	self.llegada = llegadaParam,
                	console.log('Que es esto '+JSON.stringify(llegadaParam));console.log('Form pedido '+JSON.stringify(self.llegada));
                    $http.post('http://localhost:8080/backend/ServiciosMock/llegadaComponente'
                    	, self.llegada).success(function(data){
                    	alert("Respuesta "+data.Respuesta);
                        self.llegada={};
                    });
                };
            }],
            controllerAs:'registrarLlegadaComponenteCtrl'
        };
    });
    
    prodAndes.directive('registrarEjecucionEtapaForm', function(){
        return{
            restrict:'E',
            templateUrl:'partials/registrar-ejecucion-etapa-form.html',
            controller: ['$http',function($http){
                var self=this;
                self.ejecucionEtapa={};
                this.addEjecucionEtapa=function(EjecucionEtapaP){

                	self.ejecucionEtapa = EjecucionEtapaP,
                	console.log('Form pedido '+JSON.stringify(self.ejecucionEtapa));
                    $http.post('http://localhost:8080/backend/ServiciosMock/registrarEjecucionEtapa'
                    	, self.ejecucionEtapa).success(function(data){
                    	alert("Respuesta se ha registrado la entrega del pedido");
                        self.ejecucionEtapa={};
                    });
                };
            }],
            controllerAs:'registrarEjecucionEtapaCtrl'
        };
    });
})();

