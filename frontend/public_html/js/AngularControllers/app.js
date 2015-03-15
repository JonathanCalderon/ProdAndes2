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

 })();

